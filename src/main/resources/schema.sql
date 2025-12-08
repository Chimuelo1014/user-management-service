-- ===================================
-- SENTINEL USER-MANAGEMENT-SERVICE
-- ===================================

-- Drop tables if exist
DROP TABLE IF EXISTS invitation_projects CASCADE;
DROP TABLE IF EXISTS project_members CASCADE;
DROP TABLE IF EXISTS tenant_members CASCADE;
DROP TABLE IF EXISTS invitations CASCADE;
DROP TABLE IF EXISTS user_plans CASCADE;

-- ===================================
-- TENANT_MEMBERS
-- ===================================
CREATE TABLE tenant_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    invited_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_tenant_user UNIQUE (tenant_id, user_id),
    CONSTRAINT chk_tenant_role CHECK (role IN ('TENANT_ADMIN', 'TENANT_USER'))
);

-- ===================================
-- PROJECT_MEMBERS
-- ===================================
CREATE TABLE project_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    added_by UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uq_project_user UNIQUE (project_id, user_id),
    CONSTRAINT chk_project_role CHECK (role IN ('PROJECT_ADMIN', 'PROJECT_MEMBER', 'PROJECT_VIEWER'))
);

-- ===================================
-- INVITATIONS
-- ===================================
CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    type VARCHAR(20) NOT NULL,
    resource_id UUID NOT NULL,
    resource_name VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    invited_by UUID NOT NULL,
    inviter_email VARCHAR(255),
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at TIMESTAMP,
    notification_id VARCHAR(255),
    
    CONSTRAINT chk_invitation_type CHECK (type IN ('TENANT', 'PROJECT')),
    CONSTRAINT chk_invitation_status CHECK (status IN ('PENDING', 'ACCEPTED', 'EXPIRED', 'REVOKED'))
);

-- ===================================
-- INVITATION_PROJECTS (NEW)
-- ===================================
CREATE TABLE invitation_projects (
    invitation_id UUID NOT NULL,
    project_id UUID NOT NULL,
    
    CONSTRAINT fk_invitation FOREIGN KEY (invitation_id) REFERENCES invitations(id) ON DELETE CASCADE,
    CONSTRAINT uq_invitation_project UNIQUE (invitation_id, project_id)
);

-- ===================================
-- USER_PLANS (NEW)
-- ===================================
CREATE TABLE user_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    plan VARCHAR(50) NOT NULL DEFAULT 'FREE', -- FREE, PRO, ENTERPRISE
    max_tenants INT NOT NULL DEFAULT 3,
    max_projects_per_tenant INT NOT NULL DEFAULT 5,
    max_users_per_tenant INT NOT NULL DEFAULT 10,
    max_scans_per_month INT NOT NULL DEFAULT 100,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_plan CHECK (plan IN ('FREE', 'PRO', 'ENTERPRISE'))
);

-- ===================================
-- INDEXES
-- ===================================

-- Tenant members
CREATE INDEX idx_tenant_members_tenant_id ON tenant_members(tenant_id);
CREATE INDEX idx_tenant_members_user_id ON tenant_members(user_id);
CREATE INDEX idx_tenant_members_role ON tenant_members(role);

-- Project members
CREATE INDEX idx_project_members_project_id ON project_members(project_id);
CREATE INDEX idx_project_members_user_id ON project_members(user_id);
CREATE INDEX idx_project_members_tenant_id ON project_members(tenant_id);
CREATE INDEX idx_project_members_role ON project_members(role);

-- Invitations
CREATE INDEX idx_invitations_email ON invitations(email);
CREATE INDEX idx_invitations_token ON invitations(token);
CREATE INDEX idx_invitations_status ON invitations(status);
CREATE INDEX idx_invitations_resource_id ON invitations(resource_id);
CREATE INDEX idx_invitations_type ON invitations(type);
CREATE INDEX idx_invitations_expires_at ON invitations(expires_at);

-- Invitation projects
CREATE INDEX idx_invitation_projects_invitation_id ON invitation_projects(invitation_id);
CREATE INDEX idx_invitation_projects_project_id ON invitation_projects(project_id);

-- User plans
CREATE INDEX idx_user_plans_user_id ON user_plans(user_id);
CREATE INDEX idx_user_plans_plan ON user_plans(plan);

-- ===================================
-- FUNCTIONS & TRIGGERS
-- ===================================

-- Drop existing if they exist
DROP TRIGGER IF EXISTS update_tenant_members_updated_at ON tenant_members;
DROP TRIGGER IF EXISTS update_project_members_updated_at ON project_members;
DROP FUNCTION IF EXISTS update_updated_at_column();

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers
CREATE TRIGGER update_tenant_members_updated_at 
BEFORE UPDATE ON tenant_members
FOR EACH ROW 
EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_project_members_updated_at 
BEFORE UPDATE ON project_members
FOR EACH ROW 
EXECUTE FUNCTION update_updated_at_column();

-- ===================================
-- TENANTS (Remove plan column)
-- ===================================
-- Modificar tabla existente
ALTER TABLE tenants DROP COLUMN IF EXISTS plan;
ALTER TABLE tenants DROP COLUMN IF EXISTS max_users;
ALTER TABLE tenants DROP COLUMN IF EXISTS max_projects;

-- Agregar columna de owner
ALTER TABLE tenants ADD COLUMN owner_id UUID NOT NULL;
ALTER TABLE tenants ADD CONSTRAINT fk_tenant_owner FOREIGN KEY (owner_id) REFERENCES users(id);