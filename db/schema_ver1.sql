-- =========================================================
-- SEAL Hackathon Management System - MySQL 8 Schema (v3 compatibility fixed)
-- DB-first version based on SEAL SRS v1.2 and our ERD discussion.
-- v2 fix: removed CHECK on criteria_sets.event_id/round_id because some MySQL versions reject CHECK + FK referential actions; replaced it with triggers.
-- v3 fix: removed generated scope columns used in unique constraints to improve compatibility with MySQL/MariaDB GUI runners.
--
-- IMPORTANT:
-- =========================================================

SET NAMES utf8mb4;
SET time_zone = '+07:00';

-- ============================================================
-- REBUILD-SAFE cho DB DÙNG CHUNG.
-- HÃY KẾT NỐI tới database chung của nhóm TRƯỚC khi chạy (vd: USE seal_db;).
-- Script KHÔNG tạo/xoá database, chỉ xoá & tạo lại các bảng.
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS category_resources;
DROP TABLE IF EXISTS rbl_metric_results;
DROP TABLE IF EXISTS rbl_analysis_runs;
DROP TABLE IF EXISTS audit_logs;
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS result_publications;
DROP TABLE IF EXISTS awards;
DROP TABLE IF EXISTS rankings;
DROP TABLE IF EXISTS scores;
DROP TABLE IF EXISTS evaluations;
DROP TABLE IF EXISTS submission_versions;
DROP TABLE IF EXISTS submissions;
DROP TABLE IF EXISTS judge_assignments;
DROP TABLE IF EXISTS team_invitations;
DROP TABLE IF EXISTS team_members;
DROP TABLE IF EXISTS teams;
DROP TABLE IF EXISTS budget_items;
DROP TABLE IF EXISTS event_budgets;
DROP TABLE IF EXISTS scoring_criteria;
DROP TABLE IF EXISTS criteria_sets;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS rounds;
DROP TABLE IF EXISTS events;
DROP TABLE IF EXISTS term_plans;
DROP TABLE IF EXISTS user_role_assignments;
DROP TABLE IF EXISTS system_configs;
DROP TABLE IF EXISTS budget_categories;
DROP TABLE IF EXISTS disciplines;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
SET FOREIGN_KEY_CHECKS = 1;

-- =========================================================
-- 1. MASTER DATA / USER / ROLE
-- =========================================================

CREATE TABLE roles (
                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                       code VARCHAR(50) NOT NULL UNIQUE,
                       name VARCHAR(100) NOT NULL,
                       description VARCHAR(255),
                       is_system_role BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE users (
                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                       email VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(150) NOT NULL,
                       phone VARCHAR(30),
                       primary_role_id BIGINT UNSIGNED NULL,
                       account_type VARCHAR(30) NOT NULL DEFAULT 'PARTICIPANT',
                       student_id VARCHAR(50),
                       university VARCHAR(150),
                       is_fpt_student BOOLEAN NOT NULL DEFAULT FALSE,
                       status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                       approved_by BIGINT UNSIGNED NULL,
                       approved_at TIMESTAMP NULL,
                       locked_reason VARCHAR(255),
                       last_login_at TIMESTAMP NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT fk_users_primary_role
                           FOREIGN KEY (primary_role_id) REFERENCES roles(id)
                               ON DELETE SET NULL ON UPDATE CASCADE,
                       CONSTRAINT fk_users_approved_by
                           FOREIGN KEY (approved_by) REFERENCES users(id)
                               ON DELETE SET NULL ON UPDATE CASCADE,
                       CONSTRAINT chk_users_account_type
                           CHECK (account_type IN ('PARTICIPANT', 'STAFF', 'GUEST_JUDGE')),
                       CONSTRAINT chk_users_status
                           CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'LOCKED', 'REJECTED'))
) ENGINE=InnoDB;

CREATE TABLE disciplines (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             code VARCHAR(30) NOT NULL,
                             name VARCHAR(150) NOT NULL,
                             description TEXT,
                             is_active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_by BIGINT UNSIGNED NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT uq_disciplines_code UNIQUE (code),
                             CONSTRAINT fk_disciplines_created_by
                                 FOREIGN KEY (created_by) REFERENCES users(id)
                                     ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE budget_categories (
                                   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                   code VARCHAR(50) NOT NULL,
                                   name VARCHAR(100) NOT NULL,
                                   description VARCHAR(255),
                                   is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT uq_budget_categories_code UNIQUE (code)
) ENGINE=InnoDB;

CREATE TABLE system_configs (
                                id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                config_key VARCHAR(100) NOT NULL,
                                config_value TEXT NOT NULL,
                                description VARCHAR(255),
                                updated_by BIGINT UNSIGNED NULL,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                CONSTRAINT uq_system_configs_key UNIQUE (config_key),
                                CONSTRAINT fk_system_configs_updated_by
                                    FOREIGN KEY (updated_by) REFERENCES users(id)
                                        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Scoped role assignment:
-- Used when a user can be Coordinator/Judge/Mentor/Leader/Member in a specific Event/Round/Category.
CREATE TABLE user_role_assignments (
                                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                       user_id BIGINT UNSIGNED NOT NULL,
                                       role_id BIGINT UNSIGNED NOT NULL,
                                       event_id BIGINT UNSIGNED NULL,
                                       round_id BIGINT UNSIGNED NULL,
                                       category_id BIGINT UNSIGNED NULL,
                                       assigned_by BIGINT UNSIGNED NULL,
                                       assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                       revoked_at TIMESTAMP NULL,
                                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    -- In v3, nullable scope fields are kept simple for maximum MySQL/MariaDB compatibility.
    -- Duplicate scoped-role prevention should be validated in service layer.

                                       CONSTRAINT fk_ura_user
                                           FOREIGN KEY (user_id) REFERENCES users(id)
                                               ON DELETE RESTRICT ON UPDATE CASCADE,
                                       CONSTRAINT fk_ura_role
                                           FOREIGN KEY (role_id) REFERENCES roles(id)
                                               ON DELETE RESTRICT ON UPDATE CASCADE,
                                       CONSTRAINT fk_ura_assigned_by
                                           FOREIGN KEY (assigned_by) REFERENCES users(id)
                                               ON DELETE SET NULL ON UPDATE CASCADE,
                                       CONSTRAINT chk_ura_status
                                           CHECK (status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB;

-- Event/round/category FKs for user_role_assignments are added later,
-- after those tables are created.

-- =========================================================
-- 2. PROGRAM GOVERNANCE: TERM PLAN, EVENT
-- =========================================================

CREATE TABLE term_plans (
                            id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                            term VARCHAR(20) NOT NULL,
                            year SMALLINT UNSIGNED NOT NULL,
                            discipline_id BIGINT UNSIGNED NOT NULL,
                            max_events INT UNSIGNED NOT NULL DEFAULT 1,
                            created_by BIGINT UNSIGNED NULL,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            CONSTRAINT uq_term_plans_term_year_discipline UNIQUE (term, year, discipline_id),
                            CONSTRAINT fk_term_plans_discipline
                                FOREIGN KEY (discipline_id) REFERENCES disciplines(id)
                                    ON DELETE RESTRICT ON UPDATE CASCADE,
                            CONSTRAINT fk_term_plans_created_by
                                FOREIGN KEY (created_by) REFERENCES users(id)
                                    ON DELETE SET NULL ON UPDATE CASCADE,
                            CONSTRAINT chk_term_plans_term
                                CHECK (term IN ('SPRING', 'SUMMER', 'FALL')),
                            CONSTRAINT chk_term_plans_max_events
                                CHECK (max_events > 0)
) ENGINE=InnoDB;

CREATE TABLE events (
                        id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                        name VARCHAR(200) NOT NULL,
                        slug VARCHAR(220) NOT NULL,
                        event_type VARCHAR(20) NOT NULL,
                        discipline_id BIGINT UNSIGNED NOT NULL,
                        term_plan_id BIGINT UNSIGNED NOT NULL,
                        description TEXT,
                        registration_start TIMESTAMP NULL,
                        registration_end TIMESTAMP NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                        owner_coordinator_id BIGINT UNSIGNED NOT NULL,
                        created_by BIGINT UNSIGNED NOT NULL,
                        submitted_at TIMESTAMP NULL,
                        approved_by BIGINT UNSIGNED NULL,
                        approved_at TIMESTAMP NULL,
                        rejection_reason TEXT,
                        archived_at TIMESTAMP NULL,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        CONSTRAINT uq_events_slug UNIQUE (slug),
                        CONSTRAINT uq_events_name_term UNIQUE (name, term_plan_id),

                        CONSTRAINT fk_events_discipline
                            FOREIGN KEY (discipline_id) REFERENCES disciplines(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_events_term_plan
                            FOREIGN KEY (term_plan_id) REFERENCES term_plans(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_events_owner
                            FOREIGN KEY (owner_coordinator_id) REFERENCES users(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_events_created_by
                            FOREIGN KEY (created_by) REFERENCES users(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_events_approved_by
                            FOREIGN KEY (approved_by) REFERENCES users(id)
                                ON DELETE SET NULL ON UPDATE CASCADE,

                        CONSTRAINT chk_events_type
                            CHECK (event_type IN ('SPRING', 'SUMMER', 'FALL', 'SPECIAL')),
                        CONSTRAINT chk_events_status
                            CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'REJECTED', 'APPROVED',
                                              'OPEN', 'IN_PROGRESS', 'COMPLETED', 'ARCHIVED')),
                        CONSTRAINT chk_events_registration_window
                            CHECK (registration_start IS NULL OR registration_end IS NULL OR registration_start < registration_end)
) ENGINE=InnoDB;

-- =========================================================
-- 3. EVENT CONFIGURATION: ROUND, CATEGORY, CRITERIA TEMPLATE/ACTUAL
-- =========================================================

CREATE TABLE rounds (
                        id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                        event_id BIGINT UNSIGNED NOT NULL,
                        name VARCHAR(150) NOT NULL,
                        order_number INT UNSIGNED NOT NULL,
                        submission_deadline TIMESTAMP NULL,
                        scoring_deadline TIMESTAMP NULL,
                        status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                        promotion_top_n INT UNSIGNED NULL,
                        is_final_round BOOLEAN NOT NULL DEFAULT FALSE,
                        requires_repo BOOLEAN NOT NULL DEFAULT TRUE,
                        requires_demo BOOLEAN NOT NULL DEFAULT FALSE,
                        requires_slide BOOLEAN NOT NULL DEFAULT FALSE,
                        requires_report BOOLEAN NOT NULL DEFAULT FALSE,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        CONSTRAINT uq_rounds_event_order UNIQUE (event_id, order_number),
                        CONSTRAINT uq_rounds_event_name UNIQUE (event_id, name),
                        CONSTRAINT fk_rounds_event
                            FOREIGN KEY (event_id) REFERENCES events(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT chk_rounds_status
                            CHECK (status IN ('DRAFT', 'OPEN_FOR_SUBMISSION', 'SUBMISSION_CLOSED',
                                              'SCORING_OPEN', 'SCORING_LOCKED', 'COMPLETED'))
) ENGINE=InnoDB;

CREATE TABLE categories (
                            id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                            event_id BIGINT UNSIGNED NOT NULL,
                            name VARCHAR(150) NOT NULL,
                            description TEXT,
                            mentor_id BIGINT UNSIGNED NULL,
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                            CONSTRAINT uq_categories_event_name UNIQUE (event_id, name),
                            CONSTRAINT fk_categories_event
                                FOREIGN KEY (event_id) REFERENCES events(id)
                                    ON DELETE RESTRICT ON UPDATE CASCADE,
                            CONSTRAINT fk_categories_mentor
                                FOREIGN KEY (mentor_id) REFERENCES users(id)
                                    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE category_resources (
                                    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                    category_id BIGINT UNSIGNED NOT NULL,
                                    label VARCHAR(150),
                                    url VARCHAR(500) NOT NULL,
                                    resource_type VARCHAR(50) NOT NULL DEFAULT 'OTHER',
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                    CONSTRAINT fk_category_resources_category
                                        FOREIGN KEY (category_id) REFERENCES categories(id)
                                            ON DELETE RESTRICT ON UPDATE CASCADE,
                                    CONSTRAINT chk_category_resources_type
                                        CHECK (resource_type IN ('DATASET','DOC','SAMPLE','LINK','OTHER'))
) ENGINE=InnoDB;


CREATE TABLE criteria_sets (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               name VARCHAR(150) NOT NULL,
                               description TEXT,
                               event_id BIGINT UNSIGNED NULL,
                               round_id BIGINT UNSIGNED NULL,
                               category_id BIGINT UNSIGNED NULL,
                               promotion_top_n INT UNSIGNED NULL,
                               is_template BOOLEAN NOT NULL DEFAULT FALSE,
                               is_default BOOLEAN NOT NULL DEFAULT FALSE,
                               created_by BIGINT UNSIGNED NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               CONSTRAINT fk_criteria_sets_event
                                   FOREIGN KEY (event_id) REFERENCES events(id)
                                       ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_criteria_sets_round
                                   FOREIGN KEY (round_id) REFERENCES rounds(id)
                                       ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_criteria_sets_category
                                   FOREIGN KEY (category_id) REFERENCES categories(id)
                                       ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_criteria_sets_created_by
                                   FOREIGN KEY (created_by) REFERENCES users(id)
                                       ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE scoring_criteria (
                                  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                  criteria_set_id BIGINT UNSIGNED NOT NULL,
                                  name VARCHAR(150) NOT NULL,
                                  description TEXT,
                                  max_score DECIMAL(5,2) NOT NULL DEFAULT 10.00,
                                  weight DECIMAL(5,2) NOT NULL,
                                  display_order INT UNSIGNED NOT NULL DEFAULT 1,
                                  is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                  CONSTRAINT uq_scoring_criteria_set_name UNIQUE (criteria_set_id, name),
                                  CONSTRAINT uq_scoring_criteria_set_order UNIQUE (criteria_set_id, display_order),
                                  CONSTRAINT fk_scoring_criteria_set
                                      FOREIGN KEY (criteria_set_id) REFERENCES criteria_sets(id)
                                          ON DELETE RESTRICT ON UPDATE CASCADE,
                                  CONSTRAINT chk_scoring_criteria_max_score
                                      CHECK (max_score > 0),
                                  CONSTRAINT chk_scoring_criteria_weight
                                      CHECK (weight >= 0 AND weight <= 100)
) ENGINE=InnoDB;

-- Add remaining scoped role FKs after events/rounds/categories exist.
ALTER TABLE user_role_assignments
    ADD CONSTRAINT fk_ura_event
        FOREIGN KEY (event_id) REFERENCES events(id)
            ON DELETE CASCADE ON UPDATE CASCADE,
    ADD CONSTRAINT fk_ura_round
        FOREIGN KEY (round_id) REFERENCES rounds(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    ADD CONSTRAINT fk_ura_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
        ON DELETE CASCADE ON UPDATE CASCADE;

-- =========================================================
-- 4. BUDGET
-- =========================================================

CREATE TABLE event_budgets (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               event_id BIGINT UNSIGNED NOT NULL,
                               currency VARCHAR(10) NOT NULL DEFAULT 'VND',
                               total_estimated_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                               status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                               approved_by BIGINT UNSIGNED NULL,
                               approved_at TIMESTAMP NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                               CONSTRAINT uq_event_budgets_event UNIQUE (event_id),
                               CONSTRAINT fk_event_budgets_event
                                   FOREIGN KEY (event_id) REFERENCES events(id)
                                       ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_event_budgets_approved_by
                                   FOREIGN KEY (approved_by) REFERENCES users(id)
                                       ON DELETE SET NULL ON UPDATE CASCADE,
                               CONSTRAINT chk_event_budgets_status
                                   CHECK (status IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED',
                                                     'REJECTED', 'REQUIRES_REAPPROVAL')),
                               CONSTRAINT chk_event_budgets_total
                                   CHECK (total_estimated_cost >= 0)
) ENGINE=InnoDB;

CREATE TABLE budget_items (
                              id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                              budget_id BIGINT UNSIGNED NOT NULL,
                              category_id BIGINT UNSIGNED NOT NULL,
                              description VARCHAR(255) NOT NULL,
                              quantity DECIMAL(10,2) NOT NULL DEFAULT 1.00,
                              unit_cost DECIMAL(15,2) NOT NULL DEFAULT 0.00,
                              amount DECIMAL(15,2) GENERATED ALWAYS AS (quantity * unit_cost) STORED,
                              notes TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                              CONSTRAINT fk_budget_items_budget
                                  FOREIGN KEY (budget_id) REFERENCES event_budgets(id)
                                      ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT fk_budget_items_category
                                  FOREIGN KEY (category_id) REFERENCES budget_categories(id)
                                      ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT chk_budget_items_quantity
                                  CHECK (quantity > 0),
                              CONSTRAINT chk_budget_items_unit_cost
                                  CHECK (unit_cost >= 0)
) ENGINE=InnoDB;

-- =========================================================
-- 5. TEAM, MEMBERSHIP, INVITE
-- =========================================================

CREATE TABLE teams (
                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                       event_id BIGINT UNSIGNED NOT NULL,
                       category_id BIGINT UNSIGNED NOT NULL,
                       leader_id BIGINT UNSIGNED NOT NULL,
                       name VARCHAR(150) NOT NULL,
                       description TEXT,
                       status VARCHAR(30) NOT NULL DEFAULT 'REGISTERED',
                       approved_by BIGINT UNSIGNED NULL,
                       approved_at TIMESTAMP NULL,
                       rejection_reason TEXT,
                       disqualified_reason TEXT,
                       disqualified_by BIGINT UNSIGNED NULL,
                       disqualified_at TIMESTAMP NULL,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                       CONSTRAINT uq_teams_event_name UNIQUE (event_id, name),
                       CONSTRAINT fk_teams_event
                           FOREIGN KEY (event_id) REFERENCES events(id)
                               ON DELETE RESTRICT ON UPDATE CASCADE,
                       CONSTRAINT fk_teams_category
                           FOREIGN KEY (category_id) REFERENCES categories(id)
                               ON DELETE RESTRICT ON UPDATE CASCADE,
                       CONSTRAINT fk_teams_leader
                           FOREIGN KEY (leader_id) REFERENCES users(id)
                               ON DELETE RESTRICT ON UPDATE CASCADE,
                       CONSTRAINT fk_teams_approved_by
                           FOREIGN KEY (approved_by) REFERENCES users(id)
                               ON DELETE SET NULL ON UPDATE CASCADE,
                       CONSTRAINT fk_teams_disqualified_by
                           FOREIGN KEY (disqualified_by) REFERENCES users(id)
                               ON DELETE SET NULL ON UPDATE CASCADE,
                       CONSTRAINT chk_teams_status
                           CHECK (status IN ('REGISTERED', 'APPROVED', 'REJECTED',
                                             'ACTIVE', 'DISQUALIFIED', 'WITHDRAWN'))
) ENGINE=InnoDB;

CREATE TABLE team_members (
                              team_id BIGINT UNSIGNED NOT NULL,
                              user_id BIGINT UNSIGNED NOT NULL,
                              member_role VARCHAR(30) NOT NULL DEFAULT 'MEMBER',
                              status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                              joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              left_at TIMESTAMP NULL,

                              PRIMARY KEY (team_id, user_id),
                              CONSTRAINT fk_team_members_team
                                  FOREIGN KEY (team_id) REFERENCES teams(id)
                                      ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT fk_team_members_user
                                  FOREIGN KEY (user_id) REFERENCES users(id)
                                      ON DELETE RESTRICT ON UPDATE CASCADE,
                              CONSTRAINT chk_team_members_role
                                  CHECK (member_role IN ('LEADER', 'MEMBER')),
                              CONSTRAINT chk_team_members_status
                                  CHECK (status IN ('INVITED', 'ACTIVE', 'LEFT', 'REMOVED'))
) ENGINE=InnoDB;

CREATE TABLE team_invitations (
                                  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                  team_id BIGINT UNSIGNED NOT NULL,
                                  email VARCHAR(255) NOT NULL,
                                  invited_user_id BIGINT UNSIGNED NULL,
                                  invited_by BIGINT UNSIGNED NOT NULL,
                                  status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                  token VARCHAR(128) NULL,
                                  expires_at TIMESTAMP NULL,
                                  accepted_at TIMESTAMP NULL,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                  CONSTRAINT uq_team_invitation_team_email UNIQUE (team_id, email),
                                  CONSTRAINT fk_team_invitations_team
                                      FOREIGN KEY (team_id) REFERENCES teams(id)
                                          ON DELETE RESTRICT ON UPDATE CASCADE,
                                  CONSTRAINT fk_team_invitations_invited_user
                                      FOREIGN KEY (invited_user_id) REFERENCES users(id)
                                          ON DELETE SET NULL ON UPDATE CASCADE,
                                  CONSTRAINT fk_team_invitations_invited_by
                                      FOREIGN KEY (invited_by) REFERENCES users(id)
                                          ON DELETE RESTRICT ON UPDATE CASCADE,
                                  CONSTRAINT chk_team_invitations_status
                                      CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'CANCELLED'))
) ENGINE=InnoDB;

-- =========================================================
-- 6. JUDGE ASSIGNMENT AND SUBMISSION ATTEMPTS
-- =========================================================

CREATE TABLE judge_assignments (
                                   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                   judge_id BIGINT UNSIGNED NOT NULL,
                                   event_id BIGINT UNSIGNED NOT NULL,
                                   round_id BIGINT UNSIGNED NOT NULL,
                                   category_id BIGINT UNSIGNED NULL,
                                   assigned_by BIGINT UNSIGNED NOT NULL,
                                   assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    -- Duplicate judge assignment should be validated in service layer.
                                   INDEX idx_judge_assignment_scope (judge_id, round_id, category_id),

                                   CONSTRAINT fk_judge_assignments_judge
                                       FOREIGN KEY (judge_id) REFERENCES users(id)
                                           ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT fk_judge_assignments_event
                                       FOREIGN KEY (event_id) REFERENCES events(id)
                                           ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT fk_judge_assignments_round
                                       FOREIGN KEY (round_id) REFERENCES rounds(id)
                                           ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT fk_judge_assignments_category
                                       FOREIGN KEY (category_id) REFERENCES categories(id)
                                           ON DELETE SET NULL ON UPDATE CASCADE,
                                   CONSTRAINT fk_judge_assignments_assigned_by
                                       FOREIGN KEY (assigned_by) REFERENCES users(id)
                                           ON DELETE RESTRICT ON UPDATE CASCADE,
                                   CONSTRAINT chk_judge_assignments_status
                                       CHECK (status IN ('ACTIVE', 'REVOKED'))
) ENGINE=InnoDB;

CREATE TABLE submissions (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             team_id BIGINT UNSIGNED NOT NULL,
                             round_id BIGINT UNSIGNED NOT NULL,
                             submitted_by BIGINT UNSIGNED NOT NULL,
                             attempt_number INT UNSIGNED NOT NULL,
                             repo_url VARCHAR(500),
                             demo_url VARCHAR(500),
                             slide_url VARCHAR(500),
                             report_url VARCHAR(500),
                             change_note VARCHAR(255),
                             status VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED',
                             submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             github_metadata JSON NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT uq_submissions_team_round_attempt UNIQUE (team_id, round_id, attempt_number),
                             CONSTRAINT fk_submissions_team
                                 FOREIGN KEY (team_id) REFERENCES teams(id)
                                     ON DELETE RESTRICT ON UPDATE CASCADE,
                             CONSTRAINT fk_submissions_round
                                 FOREIGN KEY (round_id) REFERENCES rounds(id)
                                     ON DELETE RESTRICT ON UPDATE CASCADE,
                             CONSTRAINT fk_submissions_submitted_by
                                 FOREIGN KEY (submitted_by) REFERENCES users(id)
                                     ON DELETE RESTRICT ON UPDATE CASCADE,
                             CONSTRAINT chk_submissions_status
                                 CHECK (status IN ('SUBMITTED', 'LATE_REJECTED', 'LOCKED', 'DISQUALIFIED'))
) ENGINE=InnoDB;

-- =========================================================
-- 7. EVALUATION, SCORE, RANKING, AWARD, PUBLICATION
-- =========================================================

CREATE TABLE evaluations (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             judge_assignment_id BIGINT UNSIGNED NULL,
                             judge_id BIGINT UNSIGNED NOT NULL,
                             submission_id BIGINT UNSIGNED NOT NULL,
                             round_id BIGINT UNSIGNED NOT NULL,
                             status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                             general_comment TEXT,
                             started_at TIMESTAMP NULL,
                             submitted_at TIMESTAMP NULL,
                             locked_at TIMESTAMP NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                             CONSTRAINT uq_evaluations_judge_submission UNIQUE (judge_id, submission_id),
                             CONSTRAINT fk_evaluations_judge_assignment
                                 FOREIGN KEY (judge_assignment_id) REFERENCES judge_assignments(id)
                                     ON DELETE SET NULL ON UPDATE CASCADE,
                             CONSTRAINT fk_evaluations_judge
                                 FOREIGN KEY (judge_id) REFERENCES users(id)
                                     ON DELETE RESTRICT ON UPDATE CASCADE,
                             CONSTRAINT fk_evaluations_submission
                                 FOREIGN KEY (submission_id) REFERENCES submissions(id)
                                     ON DELETE RESTRICT ON UPDATE CASCADE,
                             CONSTRAINT fk_evaluations_round
                                 FOREIGN KEY (round_id) REFERENCES rounds(id)
                                     ON DELETE RESTRICT ON UPDATE CASCADE,
                             CONSTRAINT chk_evaluations_status
                                 CHECK (status IN ('DRAFT', 'SUBMITTED', 'LOCKED'))
) ENGINE=InnoDB;

CREATE TABLE scores (
                        id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                        evaluation_id BIGINT UNSIGNED NOT NULL,
                        criterion_id BIGINT UNSIGNED NOT NULL,
                        score_value DECIMAL(5,2) NOT NULL,
                        comment TEXT,
                        scored_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                        CONSTRAINT uq_scores_evaluation_criterion UNIQUE (evaluation_id, criterion_id),
                        CONSTRAINT fk_scores_evaluation
                            FOREIGN KEY (evaluation_id) REFERENCES evaluations(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_scores_criterion
                            FOREIGN KEY (criterion_id) REFERENCES scoring_criteria(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT chk_scores_value
                            CHECK (score_value >= 0)
) ENGINE=InnoDB;

CREATE TABLE rankings (
                          id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          event_id BIGINT UNSIGNED NOT NULL,
                          round_id BIGINT UNSIGNED NOT NULL,
                          category_id BIGINT UNSIGNED NULL,
                          team_id BIGINT UNSIGNED NOT NULL,
                          total_score DECIMAL(8,3) NOT NULL DEFAULT 0.000,
                          rank_position INT UNSIGNED NOT NULL,
                          is_promoted BOOLEAN NOT NULL DEFAULT FALSE,
                          computed_by BIGINT UNSIGNED NULL,
                          computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          snapshot_note VARCHAR(255),

    -- Ranking uniqueness should be validated in service layer because category_id can be NULL for event-level ranking.
                          INDEX idx_rankings_scope_team (round_id, category_id, team_id),
                          INDEX idx_rankings_scope_position (round_id, category_id, rank_position),

                          CONSTRAINT fk_rankings_event
                              FOREIGN KEY (event_id) REFERENCES events(id)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,
                          CONSTRAINT fk_rankings_round
                              FOREIGN KEY (round_id) REFERENCES rounds(id)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,
                          CONSTRAINT fk_rankings_category
                              FOREIGN KEY (category_id) REFERENCES categories(id)
                                  ON DELETE SET NULL ON UPDATE CASCADE,
                          CONSTRAINT fk_rankings_team
                              FOREIGN KEY (team_id) REFERENCES teams(id)
                                  ON DELETE RESTRICT ON UPDATE CASCADE,
                          CONSTRAINT fk_rankings_computed_by
                              FOREIGN KEY (computed_by) REFERENCES users(id)
                                  ON DELETE SET NULL ON UPDATE CASCADE,
                          CONSTRAINT chk_rankings_total_score
                              CHECK (total_score >= 0)
) ENGINE=InnoDB;

CREATE TABLE awards (
                        id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                        event_id BIGINT UNSIGNED NOT NULL,
                        team_id BIGINT UNSIGNED NOT NULL,
                        ranking_id BIGINT UNSIGNED NULL,
                        award_type VARCHAR(50) NOT NULL,
                        description TEXT,
                        awarded_by BIGINT UNSIGNED NOT NULL,
                        awarded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                        CONSTRAINT uq_awards_event_team_type UNIQUE (event_id, team_id, award_type),
                        CONSTRAINT fk_awards_event
                            FOREIGN KEY (event_id) REFERENCES events(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_awards_team
                            FOREIGN KEY (team_id) REFERENCES teams(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT fk_awards_ranking
                            FOREIGN KEY (ranking_id) REFERENCES rankings(id)
                                ON DELETE SET NULL ON UPDATE CASCADE,
                        CONSTRAINT fk_awards_awarded_by
                            FOREIGN KEY (awarded_by) REFERENCES users(id)
                                ON DELETE RESTRICT ON UPDATE CASCADE,
                        CONSTRAINT chk_awards_type
                            CHECK (award_type IN ('FIRST_PLACE', 'SECOND_PLACE', 'THIRD_PLACE',
                                                  'SPECIAL', 'BEST_TECHNICAL', 'BEST_PRESENTATION'))
) ENGINE=InnoDB;

CREATE TABLE result_publications (
                                     id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                     event_id BIGINT UNSIGNED NOT NULL,
                                     round_id BIGINT UNSIGNED NULL,
                                     category_id BIGINT UNSIGNED NULL,
                                     publication_type VARCHAR(50) NOT NULL DEFAULT 'ROUND_RESULT',
                                     title VARCHAR(200) NOT NULL,
                                     description TEXT,
                                     is_public BOOLEAN NOT NULL DEFAULT FALSE,
                                     published_by BIGINT UNSIGNED NULL,
                                     published_at TIMESTAMP NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                     CONSTRAINT fk_result_publications_event
                                         FOREIGN KEY (event_id) REFERENCES events(id)
                                             ON DELETE RESTRICT ON UPDATE CASCADE,
                                     CONSTRAINT fk_result_publications_round
                                         FOREIGN KEY (round_id) REFERENCES rounds(id)
                                             ON DELETE SET NULL ON UPDATE CASCADE,
                                     CONSTRAINT fk_result_publications_category
                                         FOREIGN KEY (category_id) REFERENCES categories(id)
                                             ON DELETE SET NULL ON UPDATE CASCADE,
                                     CONSTRAINT fk_result_publications_published_by
                                         FOREIGN KEY (published_by) REFERENCES users(id)
                                             ON DELETE SET NULL ON UPDATE CASCADE,
                                     CONSTRAINT chk_result_publications_type
                                         CHECK (publication_type IN ('ROUND_RESULT', 'FINAL_RESULT', 'AWARD_RESULT'))
) ENGINE=InnoDB;

-- =========================================================
-- =========================================================

CREATE TABLE notifications (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               recipient_id BIGINT UNSIGNED NOT NULL,
                               event_id BIGINT UNSIGNED NULL,
                               notification_type VARCHAR(50) NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               message TEXT NOT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               read_at TIMESTAMP NULL,

                               CONSTRAINT fk_notifications_recipient
                                   FOREIGN KEY (recipient_id) REFERENCES users(id)
                                       ON DELETE RESTRICT ON UPDATE CASCADE,
                               CONSTRAINT fk_notifications_event
                                   FOREIGN KEY (event_id) REFERENCES events(id)
                                       ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
                            id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                            actor_id BIGINT UNSIGNED NULL,
                            action_type VARCHAR(100) NOT NULL,
                            target_type VARCHAR(100) NOT NULL,
                            target_id BIGINT UNSIGNED NULL,
                            old_value JSON NULL,
                            new_value JSON NULL,
                            reason TEXT,
                            ip_address VARCHAR(45),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                            CONSTRAINT fk_audit_logs_actor
                                FOREIGN KEY (actor_id) REFERENCES users(id)
                                    ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB;

-- =========================================================
-- 9. INDEXES FOR COMMON QUERIES
-- =========================================================

CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_primary_role ON users(primary_role_id);

CREATE INDEX idx_events_discipline_term_status ON events(discipline_id, term_plan_id, status);
CREATE INDEX idx_events_owner ON events(owner_coordinator_id);

CREATE INDEX idx_rounds_event_order ON rounds(event_id, order_number);
CREATE INDEX idx_categories_event ON categories(event_id);
CREATE INDEX idx_criteria_sets_event_round ON criteria_sets(event_id, round_id);

CREATE INDEX idx_event_budgets_event_status ON event_budgets(event_id, status);
CREATE INDEX idx_budget_items_budget ON budget_items(budget_id);

CREATE INDEX idx_teams_event_category_status ON teams(event_id, category_id, status);
CREATE INDEX idx_team_members_user ON team_members(user_id);
CREATE INDEX idx_judge_assignments_judge_round ON judge_assignments(judge_id, round_id);

CREATE INDEX idx_submissions_team_round_attempt ON submissions(team_id, round_id, attempt_number);

CREATE INDEX idx_evaluations_judge ON evaluations(judge_id);
CREATE INDEX idx_evaluations_submission ON evaluations(submission_id);
CREATE INDEX idx_scores_criterion ON scores(criterion_id);

CREATE INDEX idx_rankings_event_round_category ON rankings(event_id, round_id, category_id);
CREATE INDEX idx_awards_event_team ON awards(event_id, team_id);

CREATE INDEX idx_notifications_recipient_read ON notifications(recipient_id, is_read);
CREATE INDEX idx_audit_logs_actor_created ON audit_logs(actor_id, created_at);
CREATE INDEX idx_audit_logs_target ON audit_logs(target_type, target_id);

-- =========================================================
-- 10. TRIGGERS
-- =========================================================

DELIMITER $$

CREATE TRIGGER trg_criteria_sets_validate_insert
    BEFORE INSERT ON criteria_sets
    FOR EACH ROW
BEGIN
    IF (
        (NEW.is_template = TRUE AND (NEW.event_id IS NOT NULL OR NEW.round_id IS NOT NULL))
        OR
        (NEW.is_template = FALSE AND NEW.event_id IS NULL AND NEW.round_id IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid criteria_sets scope: template must not have event_id/round_id; actual set must have event_id or round_id';
END IF;
END$$

CREATE TRIGGER trg_criteria_sets_validate_update
    BEFORE UPDATE ON criteria_sets
    FOR EACH ROW
BEGIN
    IF (
        (NEW.is_template = TRUE AND (NEW.event_id IS NOT NULL OR NEW.round_id IS NOT NULL))
        OR
        (NEW.is_template = FALSE AND NEW.event_id IS NULL AND NEW.round_id IS NULL)
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Invalid criteria_sets scope: template must not have event_id/round_id; actual set must have event_id or round_id';
END IF;
END$$

CREATE TRIGGER trg_audit_logs_prevent_update
    BEFORE UPDATE ON audit_logs
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_logs are append-only and cannot be updated';
END$$

    CREATE TRIGGER trg_audit_logs_prevent_delete
        BEFORE DELETE ON audit_logs
        FOR EACH ROW
    BEGIN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'audit_logs are append-only and cannot be deleted';
END$$

        CREATE TRIGGER trg_budget_items_after_insert
            AFTER INSERT ON budget_items
            FOR EACH ROW
        BEGIN
            UPDATE event_budgets
            SET total_estimated_cost = (
                SELECT COALESCE(SUM(amount), 0)
                FROM budget_items
                WHERE budget_id = NEW.budget_id
            )
            WHERE id = NEW.budget_id;
            END$$

            CREATE TRIGGER trg_budget_items_after_update
                AFTER UPDATE ON budget_items
                FOR EACH ROW
            BEGIN
                UPDATE event_budgets
                SET total_estimated_cost = (
                    SELECT COALESCE(SUM(amount), 0)
                    FROM budget_items
                    WHERE budget_id = NEW.budget_id
                )
                WHERE id = NEW.budget_id;

                IF OLD.budget_id <> NEW.budget_id THEN
                UPDATE event_budgets
                SET total_estimated_cost = (
                    SELECT COALESCE(SUM(amount), 0)
                    FROM budget_items
                    WHERE budget_id = OLD.budget_id
                )
                WHERE id = OLD.budget_id;
            END IF;
            END$$

            CREATE TRIGGER trg_budget_items_after_delete
                AFTER DELETE ON budget_items
                FOR EACH ROW
            BEGIN
                UPDATE event_budgets
                SET total_estimated_cost = (
                    SELECT COALESCE(SUM(amount), 0)
                    FROM budget_items
                    WHERE budget_id = OLD.budget_id
                )
                WHERE id = OLD.budget_id;
                END$$

                DELIMITER ;

-- =========================================================
-- 11. SEED DATA
-- =========================================================

-- Roles
                INSERT INTO roles (id, code, name, description) VALUES
                                                                    (1, 'ADMIN', 'Admin', 'Platform administrator'),
                                                                    (2, 'SUPER_COORDINATOR', 'Super Coordinator', 'Program-level authority'),
                                                                    (3, 'COORDINATOR', 'Event Coordinator', 'Owns and operates events'),
                                                                    (4, 'JUDGE', 'Judge', 'Scores assigned submissions'),
                                                                    (5, 'MENTOR', 'Mentor', 'Guides teams in assigned category'),
                                                                    (6, 'TEAM_LEADER', 'Team Leader', 'Team representative'),
                                                                    (7, 'TEAM_MEMBER', 'Team Member', 'Competition participant');

                -- Users
-- Replace password_hash values with real BCrypt hashes in your application.
                INSERT INTO users
                (id, email, password_hash, full_name, phone, primary_role_id, account_type,
                 student_id, university, is_fpt_student, status, approved_by, approved_at)
                VALUES
                    (1, 'admin@seal.local', '$2a$10$replace_with_real_bcrypt_hash_admin', 'System Admin', '0900000001', 1, 'STAFF', NULL, 'FPT University HCMC', FALSE, 'ACTIVE', NULL, CURRENT_TIMESTAMP),
                    (2, 'super@seal.local', '$2a$10$replace_with_real_bcrypt_hash_super', 'Super Coordinator', '0900000002', 2, 'STAFF', NULL, 'FPT University HCMC', FALSE, 'ACTIVE', 1, CURRENT_TIMESTAMP),
                    (3, 'coord@seal.local', '$2a$10$replace_with_real_bcrypt_hash_coord', 'Event Coordinator', '0900000003', 3, 'STAFF', NULL, 'FPT University HCMC', FALSE, 'ACTIVE', 1, CURRENT_TIMESTAMP),
                    (4, 'judge.internal@seal.local', '$2a$10$replace_with_real_bcrypt_hash_judge', 'Internal Judge', '0900000004', 4, 'STAFF', NULL, 'FPT University HCMC', FALSE, 'ACTIVE', 1, CURRENT_TIMESTAMP),
                    (5, 'guest.judge@seal.local', '$2a$10$replace_with_real_bcrypt_hash_guest', 'Guest Judge', '0900000005', 4, 'GUEST_JUDGE', NULL, 'Industry Partner', FALSE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                    (6, 'mentor@seal.local', '$2a$10$replace_with_real_bcrypt_hash_mentor', 'Faculty Mentor', '0900000006', 5, 'STAFF', NULL, 'FPT University HCMC', FALSE, 'ACTIVE', 1, CURRENT_TIMESTAMP),
                    (7, 'leader@student.local', '$2a$10$replace_with_real_bcrypt_hash_leader', 'Team Leader Demo', '0900000007', 6, 'PARTICIPANT', 'SE190001', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                    (8, 'member1@student.local', '$2a$10$replace_with_real_bcrypt_hash_member1', 'Team Member One', '0900000008', 7, 'PARTICIPANT', 'SE190002', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                    (9, 'member2@student.local', '$2a$10$replace_with_real_bcrypt_hash_member2', 'Team Member Two', '0900000009', 7, 'PARTICIPANT', 'EXT001', 'Partner University', FALSE, 'ACTIVE', 3, CURRENT_TIMESTAMP);

-- Disciplines
                INSERT INTO disciplines (id, code, name, description, is_active, created_by) VALUES
                                                                                                 (1, 'SE', 'Software Engineering', 'Software Engineering hackathon discipline', TRUE, 2),
                                                                                                 (2, 'AI', 'Artificial Intelligence', 'AI-focused hackathon discipline', TRUE, 2),
                                                                                                 (3, 'IOT', 'Internet of Things', 'IoT-focused hackathon discipline', TRUE, 2);

-- Budget Categories
                INSERT INTO budget_categories (id, code, name, description) VALUES
                                                                                (1, 'VENUE', 'Venue', 'Room, hall, and facility cost'),
                                                                                (2, 'PRIZE', 'Prize', 'Prize and award cost'),
                                                                                (3, 'GUEST_JUDGE_FEE', 'Guest Judge Fee', 'Honorarium or fee for guest judges'),
                                                                                (4, 'MATERIALS', 'Materials', 'Materials and equipment'),
                                                                                (5, 'CATERING', 'Catering', 'Food and beverage cost'),
                                                                                (6, 'MARKETING', 'Marketing', 'Marketing and promotion'),
                                                                                (7, 'OTHER', 'Other', 'Other event expenses');

-- System Config
                INSERT INTO system_configs (config_key, config_value, description, updated_by) VALUES
                                                                                                   ('JWT_ACCESS_TOKEN_MINUTES', '60', 'JWT access token expiration in minutes', 1),
                                                                                                   ('JWT_REFRESH_TOKEN_DAYS', '7', 'JWT refresh token expiration in days', 1),
                                                                                                   ('PASSWORD_MIN_LENGTH', '8', 'Minimum password length', 1),
                                                                                                   ('DEFAULT_CURRENCY', 'VND', 'Default event budget currency', 1),
                                                                                                   ('TEAM_MIN_SIZE', '3', 'Minimum team size', 1),
                                                                                                   ('TEAM_MAX_SIZE', '5', 'Maximum team size', 1);

-- Term Plan
                INSERT INTO term_plans (id, term, year, discipline_id, max_events, created_by) VALUES
                                                                                                   (1, 'SUMMER', 2026, 1, 3, 2),
                                                                                                   (2, 'SUMMER', 2026, 2, 2, 2),
                                                                                                   (3, 'FALL', 2026, 1, 3, 2);

-- Sample Event
                INSERT INTO events
                (id, name, slug, event_type, discipline_id, term_plan_id, description,
                 registration_start, registration_end, status, owner_coordinator_id, created_by,
                 submitted_at, approved_by, approved_at)
                VALUES
                    (1, 'SEAL Software Engineering Hackathon Summer 2026',
                     'seal-se-summer-2026',
                     'SUMMER',
                     1,
                     1,
                     'Demo SEAL hackathon event for Software Engineering discipline.',
                     '2026-06-01 08:00:00',
                     '2026-06-20 23:59:59',
                     'IN_PROGRESS',
                     3,
                     3,
                     '2026-05-20 09:00:00',
                     2,
                     '2026-05-21 10:00:00');

-- Rounds
                INSERT INTO rounds
                (id, event_id, name, order_number, submission_deadline, status, promotion_top_n, is_final_round,
                 requires_repo, requires_demo, requires_slide, requires_report)
                VALUES
                    (1, 1, 'Preliminary Round', 1, '2026-06-25 23:59:59', 'SCORING_OPEN', 10, FALSE,
                     TRUE, FALSE, FALSE, FALSE),
                    (2, 1, 'Final Round', 2, '2026-07-05 23:59:59', 'DRAFT', NULL, TRUE,
                     TRUE, TRUE, TRUE, TRUE);

-- Categories
                INSERT INTO categories
                (id, event_id, name, description, mentor_id, is_active)
                VALUES
                    (1, 1, 'Web Application', 'Web-based software product category', 6, TRUE),
                    (2, 1, 'Mobile Application', 'Mobile app software product category', 6, TRUE),
                    (3, 1, 'AI/Automation Tool', 'AI-assisted tool or automation category', NULL, TRUE);

-- Criteria Template
                INSERT INTO criteria_sets
                (id, name, description, event_id, round_id, is_template, is_default, created_by)
                VALUES
                    (1, 'Default Hackathon Criteria', 'Reusable default scoring criteria template.', NULL, NULL, TRUE, TRUE, 3);

                INSERT INTO scoring_criteria
                (id, criteria_set_id, name, description, max_score, weight, display_order, is_active)
                VALUES
                    (1, 1, 'Technical Quality', 'Code quality, architecture, correctness, and maintainability.', 10.00, 40.00, 1, TRUE),
                    (2, 1, 'Innovation', 'Novelty and creativity of the solution.', 10.00, 25.00, 2, TRUE),
                    (3, 1, 'UI/UX', 'Usability, accessibility, and design quality.', 10.00, 20.00, 3, TRUE),
                    (4, 1, 'Presentation', 'Clarity, demo quality, and communication.', 10.00, 15.00, 4, TRUE);

-- Actual CriteriaSet copied from template for Preliminary Round
                INSERT INTO criteria_sets
                (id, name, description, event_id, round_id, is_template, is_default, created_by)
                VALUES
                    (2, 'SEAL Summer 2026 Preliminary Criteria', 'Actual criteria for Preliminary Round.', 1, 1, FALSE, FALSE, 3);

                INSERT INTO scoring_criteria
                (id, criteria_set_id, name, description, max_score, weight, display_order, is_active)
                VALUES
                    (5, 2, 'Technical Quality', 'Code quality, architecture, correctness, and maintainability.', 10.00, 40.00, 1, TRUE),
                    (6, 2, 'Innovation', 'Novelty and creativity of the solution.', 10.00, 25.00, 2, TRUE),
                    (7, 2, 'UI/UX', 'Usability, accessibility, and design quality.', 10.00, 20.00, 3, TRUE),
                    (8, 2, 'Presentation', 'Clarity, demo quality, and communication.', 10.00, 15.00, 4, TRUE);

-- Budget
                INSERT INTO event_budgets
                (id, event_id, currency, status, approved_by, approved_at)
                VALUES
                    (1, 1, 'VND', 'APPROVED', 2, '2026-05-21 10:00:00');

                INSERT INTO budget_items
                (budget_id, category_id, description, quantity, unit_cost, notes)
                VALUES
                    (1, 2, 'First/Second/Third place prizes', 1, 12000000, 'Prize pool for final ranking'),
                    (1, 5, 'Catering for participants and judges', 80, 50000, 'Lunch and snacks'),
                    (1, 3, 'Guest judge honorarium', 1, 2000000, 'Guest judge fee'),
                    (1, 6, 'Poster and online promotion', 1, 1000000, 'Marketing materials');

-- Scoped roles for sample event
                INSERT INTO user_role_assignments
                (user_id, role_id, event_id, round_id, category_id, assigned_by, status)
                VALUES
                    (3, 3, 1, NULL, NULL, 2, 'ACTIVE'),      -- Coordinator of Event 1
                    (4, 4, 1, 1, NULL, 3, 'ACTIVE'),         -- Internal Judge for preliminary round
                    (5, 4, 1, 1, NULL, 3, 'ACTIVE'),         -- Guest Judge for preliminary round
                    (6, 5, 1, NULL, 1, 3, 'ACTIVE'),         -- Mentor for Web Application category
                    (7, 6, 1, NULL, 1, 3, 'ACTIVE'),         -- Team Leader in Event 1
                    (8, 7, 1, NULL, 1, 3, 'ACTIVE'),
                    (9, 7, 1, NULL, 1, 3, 'ACTIVE');

-- Team
                INSERT INTO teams
                (id, event_id, category_id, leader_id, name, description, status, approved_by, approved_at)
                VALUES
                    (1, 1, 1, 7, 'Code Seals', 'Demo team for Web Application category.', 'ACTIVE', 3, '2026-06-02 09:30:00');

                INSERT INTO team_members
                (team_id, user_id, member_role, status, joined_at)
                VALUES
                    (1, 7, 'LEADER', 'ACTIVE', '2026-06-02 09:00:00'),
                    (1, 8, 'MEMBER', 'ACTIVE', '2026-06-02 09:10:00'),
                    (1, 9, 'MEMBER', 'ACTIVE', '2026-06-02 09:20:00');

-- Judge assignments
                INSERT INTO judge_assignments
                (id, judge_id, event_id, round_id, category_id, assigned_by, status)
                VALUES
                    (1, 4, 1, 1, 1, 3, 'ACTIVE'),
                    (2, 5, 1, 1, 1, 3, 'ACTIVE');

-- Submission attempt
                INSERT INTO submissions
                (id, team_id, round_id, submitted_by, attempt_number, repo_url, demo_url, slide_url,
                 report_url, change_note, status, submitted_at, github_metadata)
                VALUES
                    (1, 1, 1, 7, 1,
                     'https://github.com/demo/code-seals',
                     'https://demo.seal.local/code-seals',
                     'https://docs.google.com/presentation/d/demo',
                     'https://docs.google.com/document/d/demo',
                     'Initial submission', 'SUBMITTED', '2026-06-24 20:00:00',
                     JSON_OBJECT('repository', 'https://github.com/demo/code-seals', 'language', 'Java'));

-- Evaluations and scores
                INSERT INTO evaluations
                (id, judge_assignment_id, judge_id, submission_id, round_id, status, general_comment, started_at, submitted_at)
                VALUES
                    (1, 1, 4, 1, 1, 'SUBMITTED', 'Good technical foundation and clear demo.', '2026-06-26 09:00:00', '2026-06-26 09:30:00'),
                    (2, 2, 5, 1, 1, 'SUBMITTED', 'Strong concept, needs more polish.', '2026-06-26 10:00:00', '2026-06-26 10:25:00');

                INSERT INTO scores
                (evaluation_id, criterion_id, score_value, comment)
                VALUES
                    (1, 5, 8.00, 'Good code quality.'),
                    (1, 6, 7.50, 'Interesting idea.'),
                    (1, 7, 8.00, 'Clean interface.'),
                    (1, 8, 7.00, 'Clear presentation.'),
                    (2, 5, 8.50, 'Solid architecture.'),
                    (2, 6, 8.00, 'Creative solution.'),
                    (2, 7, 8.00, 'Usable demo.'),
                    (2, 8, 7.00, 'Presentation is acceptable.');

-- Ranking snapshot
                INSERT INTO rankings
                (id, event_id, round_id, category_id, team_id, total_score, rank_position, is_promoted, computed_by, snapshot_note)
                VALUES
                    (1, 1, 1, 1, 1, 78.875, 1, TRUE, 3, 'Demo ranking computed from two judge evaluations.');

-- Award and publication
                INSERT INTO awards
                (id, event_id, team_id, ranking_id, award_type, description, awarded_by)
                VALUES
                    (1, 1, 1, 1, 'FIRST_PLACE', 'Demo first place award for Web Application category.', 3);

                INSERT INTO result_publications
                (id, event_id, round_id, category_id, publication_type, title, description, is_public, published_by, published_at)
                VALUES
                    (1, 1, 1, 1, 'ROUND_RESULT', 'Preliminary Round Results - Web Application',
                     'Demo publication for preliminary ranking.', TRUE, 3, CURRENT_TIMESTAMP);

-- Notification
                INSERT INTO notifications
                (recipient_id, event_id, notification_type, title, message, is_read)
                VALUES
                    (7, 1, 'RESULT_PUBLISHED', 'Preliminary result published',
                     'Your team result for Preliminary Round has been published.', FALSE),
                    (8, 1, 'RESULT_PUBLISHED', 'Preliminary result published',
                     'Your team result for Preliminary Round has been published.', FALSE),
                    (9, 1, 'RESULT_PUBLISHED', 'Preliminary result published',
                     'Your team result for Preliminary Round has been published.', FALSE);

-- Audit log examples
                INSERT INTO audit_logs
                (actor_id, action_type, target_type, target_id, old_value, new_value, reason, ip_address)
                VALUES
                    (3, 'EVENT_SUBMITTED', 'EVENT', 1, NULL, JSON_OBJECT('status', 'PENDING_APPROVAL'), 'Coordinator submitted event for approval.', '127.0.0.1'),
                    (2, 'EVENT_APPROVED', 'EVENT', 1, JSON_OBJECT('status', 'PENDING_APPROVAL'), JSON_OBJECT('status', 'APPROVED'), 'Approved event and budget.', '127.0.0.1'),
                    (4, 'SCORE_SUBMITTED', 'EVALUATION', 1, NULL, JSON_OBJECT('status', 'SUBMITTED'), 'Judge submitted scores.', '127.0.0.1'),
                    (3, 'RESULT_PUBLISHED', 'RESULT_PUBLICATION', 1, JSON_OBJECT('is_public', false), JSON_OBJECT('is_public', true), 'Published preliminary result.', '127.0.0.1');

                -- =========================================================
-- 12. QUICK CHECK QUERIES
-- =========================================================
-- SELECT * FROM roles;
-- SELECT * FROM users;
-- SELECT * FROM events;
-- SELECT * FROM event_budgets;
-- SELECT * FROM budget_items;
-- SELECT * FROM criteria_sets;
-- SELECT * FROM scoring_criteria;
-- SELECT * FROM teams;
-- SELECT * FROM submissions;
-- SELECT * FROM evaluations;
-- SELECT * FROM scores;
-- SELECT * FROM rankings;

-- =========================================================
-- 13. DATA MANIPULATION RANKING
-- =========================================================

-- 1. TẠO 3 USER MỚI LÀM ĐỘI TRƯỞNG (ID từ 10 đến 12)
                INSERT INTO users (id, email, password_hash, full_name, primary_role_id, account_type, status) VALUES
                                                                                                                   (10, 'leader.cyber@student.local', 'hash_stub', 'Leader Cyber Ninjas', 6, 'PARTICIPANT', 'ACTIVE'),
                                                                                                                   (11, 'leader.null@student.local', 'hash_stub', 'Leader Null Pointers', 6, 'PARTICIPANT', 'ACTIVE'),
                                                                                                                   (12, 'leader.drop@student.local', 'hash_stub', 'Leader Drop Tables', 6, 'PARTICIPANT', 'ACTIVE');

                -- 2. TẠO 3 ĐỘI THI MỚI VÀO VÒNG 1 (EVENT 1, CATEGORY 1)
-- Lưu ý: Team 4 bị DISQUALIFIED
                INSERT INTO teams (id, event_id, category_id, leader_id, name, description, status) VALUES
                                                                                                        (2, 1, 1, 10, 'Cyber Ninjas', 'Đội xuất sắc, điểm cao', 'ACTIVE'),
                                                                                                        (3, 1, 1, 11, 'Null Pointers', 'Đội trung bình, điểm thấp', 'ACTIVE'),
                                                                                                        (4, 1, 1, 12, 'Drop Tables', 'Đội vi phạm quy chế', 'DISQUALIFIED');

-- 3. TẠO BÀI NỘP CHÍNH THỨC CHO 3 ĐỘI
                INSERT INTO submissions
                (id, team_id, round_id, submitted_by, attempt_number, repo_url, status, submitted_at) VALUES
                                                                                                          (2, 2, 1, 10, 1, 'https://github.com/demo/cyber-ninjas', 'SUBMITTED', CURRENT_TIMESTAMP),
                                                                                                          (3, 3, 1, 11, 1, 'https://github.com/demo/null-pointers', 'SUBMITTED', CURRENT_TIMESTAMP),
                                                                                                          (4, 4, 1, 12, 1, 'https://github.com/demo/drop-tables', 'DISQUALIFIED', CURRENT_TIMESTAMP);

                -- 4. TẠO EVALUATIONS (Phân công Giám khảo 4 và 5 chấm điểm)
-- Judge 4 (Internal Judge) và Judge 5 (Guest Judge)
                INSERT INTO evaluations (id, judge_assignment_id, judge_id, submission_id, round_id, status) VALUES
                                                                                                                 (3, 1, 4, 2, 1, 'SUBMITTED'), -- Judge 4 chấm Team 2
                                                                                                                 (4, 2, 5, 2, 1, 'SUBMITTED'), -- Judge 5 chấm Team 2

                                                                                                                 (5, 1, 4, 3, 1, 'SUBMITTED'), -- Judge 4 chấm Team 3
                                                                                                                 (6, 2, 5, 3, 1, 'SUBMITTED'), -- Judge 5 chấm Team 3

                                                                                                                 (7, 1, 4, 4, 1, 'SUBMITTED'); -- Judge 4 chấm Team 4 (Đội bị loại)

-- 5. CHÈN ĐIỂM SỐ CHI TIẾT
-- ID Tiêu chí: 5(Tech 40%), 6(Innov 25%), 7(UI 20%), 8(Pres 15%)

-- Điểm của Team 2 (Cyber Ninjas) -> Sẽ vô địch
                INSERT INTO scores (evaluation_id, criterion_id, score_value, comment) VALUES
                                                                                           (3, 5, 9.0, 'Code xuất sắc'), (3, 6, 9.5, 'Rất sáng tạo'), (3, 7, 8.5, 'UI đẹp'), (3, 8, 9.0, 'Thuyết trình tự tin'),
                                                                                           (4, 5, 9.5, 'Kiến trúc tốt'), (4, 6, 9.0, 'Ý tưởng hay'), (4, 7, 8.5, 'Dễ dùng'), (4, 8, 8.5, 'Trình bày rõ ràng');

-- Điểm của Team 3 (Null Pointers) -> Sẽ xếp bét
                INSERT INTO scores (evaluation_id, criterion_id, score_value, comment) VALUES
                                                                                           (5, 5, 6.0, 'Nhiều bug'), (5, 6, 6.5, 'Bình thường'), (5, 7, 5.5, 'UI hơi rối'), (5, 8, 6.0, 'Thuyết trình lúng túng'),
                                                                                           (6, 5, 6.5, 'Chấp nhận được'), (6, 6, 6.0, 'Không mới mẻ'), (6, 7, 5.5, 'Khó dùng'), (6, 8, 6.5, 'Bị run');

-- Điểm của Team 4 (Drop Tables) -> Đội bị loại, cố tình cho 10 điểm để test màng lọc
                INSERT INTO scores (evaluation_id, criterion_id, score_value, comment) VALUES
                                                                                           (7, 5, 10.0, 'Nên bị loại'), (7, 6, 10.0, 'Nên bị loại'), (7, 7, 10.0, 'Nên bị loại'), (7, 8, 10.0, 'Nên bị loại');

                -- =========================================================
-- 14. DATA MANIPULATION AWARD
-- =========================================================
-- =====================================================================
-- 1. TẠO EVENT SỐ 2 (AI HACKATHON) VÀ CÁC TEAM MỚI
-- =====================================================================
                INSERT INTO events (id, name, slug, event_type, discipline_id, term_plan_id, status, owner_coordinator_id, created_by)
                VALUES (2, 'SEAL AI Hackathon Fall 2026', 'seal-ai-fall-2026', 'FALL', 2, 3, 'COMPLETED', 3, 3)
                    ON DUPLICATE KEY UPDATE name=name;

-- Tạo Category cho Event 2
                INSERT INTO categories (id, event_id, name, description, is_active)
                VALUES (4, 2, 'Computer Vision', 'Nhận diện hình ảnh', TRUE)
                    ON DUPLICATE KEY UPDATE name=name;

-- Tạo 3 Team cho Event 2 (Tái sử dụng các User ID 7, 8, 9 làm đội trưởng cho lẹ)
                INSERT INTO teams (id, event_id, category_id, leader_id, name, description, status) VALUES
                                                                                                        (5, 2, 4, 7, 'Visionaries', 'AI Đỉnh cao', 'ACTIVE'),
                                                                                                        (6, 2, 4, 8, 'Deep Minds', 'Neural Networks', 'ACTIVE'),
                                                                                                        (7, 2, 4, 9, 'Auto Bots', 'Tự động hoá', 'ACTIVE')
                    ON DUPLICATE KEY UPDATE name=name;


                -- =====================================================================
-- 2. TẠO DỮ LIỆU XẾP HẠNG (RANKINGS) CHO EVENT 2
-- =====================================================================
-- Giả lập thuật toán đã chạy xong cho Event 2
                INSERT INTO rankings (id, event_id, round_id, category_id, team_id, total_score, rank_position, is_promoted, computed_by, snapshot_note) VALUES
                                                                                                                                                             (2, 2, 2, 4, 5, 95.500, 1, TRUE, 3, 'Kết quả chung kết Event 2'),
                                                                                                                                                             (3, 2, 2, 4, 6, 88.000, 2, TRUE, 3, 'Kết quả chung kết Event 2'),
                                                                                                                                                             (4, 2, 2, 4, 7, 85.250, 3, TRUE, 3, 'Kết quả chung kết Event 2')
                    ON DUPLICATE KEY UPDATE total_score=total_score;


                -- =====================================================================
-- 3. CHÈN CÁC GIẢI THƯỞNG (AWARDS) VÀO DATABASE (ĐÃ FIX LỖI KHÓA NGOẠI)
-- =====================================================================

-- Bổ sung giải thưởng cho EVENT 1
-- Sử dụng Subquery để lấy ID xếp hạng thực tế của Team 1 thay vì hardcode số 1
                INSERT INTO awards (event_id, team_id, ranking_id, award_type, description, awarded_by) VALUES
                                                                                                            (1, 1, (SELECT id FROM rankings WHERE event_id = 1 AND team_id = 1 LIMIT 1), 'SECOND_PLACE', 'Giải Nhì - Kỹ năng lập trình web và kiến trúc cực tốt', 3),
(1, 3, NULL, 'THIRD_PLACE', 'Giải Ba - Khuyến khích ý tưởng tiềm năng', 3)
                ON DUPLICATE KEY UPDATE description=VALUES(description);

                -- =========================================================
-- 15. ADDITIONAL SUBMISSION DEMO DATA
-- =========================================================
                INSERT INTO users (id, email, password_hash, full_name, primary_role_id, account_type, student_id, university, is_fpt_student, status, approved_by, approved_at) VALUES
                                                                                                                                                                                     (13, 'leader.pitch@student.local', '$2a$10$replace_with_real_bcrypt_hash_leader13', 'Leader Pitch Deck', 6, 'PARTICIPANT', 'SE190010', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                                                                                                                                                                                     (14, 'leader.full@student.local', '$2a$10$replace_with_real_bcrypt_hash_leader14', 'Leader Full Stack', 6, 'PARTICIPANT', 'SE190011', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                                                                                                                                                                                     (15, 'member.alpha@student.local', '$2a$10$replace_with_real_bcrypt_hash_member15', 'Member Alpha', 7, 'PARTICIPANT', 'SE190012', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                                                                                                                                                                                     (16, 'member.beta@student.local', '$2a$10$replace_with_real_bcrypt_hash_member16', 'Member Beta', 7, 'PARTICIPANT', 'SE190013', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                                                                                                                                                                                     (17, 'member.gamma@student.local', '$2a$10$replace_with_real_bcrypt_hash_member17', 'Member Gamma', 7, 'PARTICIPANT', 'SE190014', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP),
                                                                                                                                                                                     (18, 'member.delta@student.local', '$2a$10$replace_with_real_bcrypt_hash_member18', 'Member Delta', 7, 'PARTICIPANT', 'SE190015', 'FPT University HCMC', TRUE, 'ACTIVE', 3, CURRENT_TIMESTAMP);

                INSERT INTO events
                (id, name, slug, event_type, discipline_id, term_plan_id, description,
                 registration_start, registration_end, status, owner_coordinator_id, created_by,
                 submitted_at, approved_by, approved_at)
                VALUES
                    (3, 'SEAL Product Showcase Summer 2026',
                     'seal-product-showcase-summer-2026',
                     'SUMMER',
                     1,
                     1,
                     'Extra demo event for slide-only and full-artifact submission scenarios.',
                     '2026-06-10 08:00:00',
                     '2026-06-22 23:59:59',
                     'IN_PROGRESS',
                     3,
                     3,
                     '2026-05-25 09:00:00',
                     2,
                     '2026-05-26 10:00:00');

                INSERT INTO categories
                (id, event_id, name, description, mentor_id, is_active)
                VALUES
                    (5, 3, 'Product Showcase', 'Presentation-focused product pitching track', 6, TRUE);

                INSERT INTO rounds
                (id, event_id, name, order_number, submission_deadline, status, promotion_top_n, is_final_round,
                 requires_repo, requires_demo, requires_slide, requires_report)
                VALUES
                    (3, 3, 'Pitch Round', 1, '2026-07-12 23:59:59', 'OPEN_FOR_SUBMISSION', 2, FALSE,
                     FALSE, FALSE, TRUE, FALSE),
                    (4, 3, 'Final Round', 2, '2026-07-20 23:59:59', 'OPEN_FOR_SUBMISSION', NULL, TRUE,
                     TRUE, TRUE, TRUE, TRUE);

                INSERT INTO teams
                (id, event_id, category_id, leader_id, name, description, status)
                VALUES
                    (8, 3, 5, 13, 'Pitch Masters', 'Team focused on polished pitch decks.', 'ACTIVE'),
                    (9, 3, 5, 14, 'Full Stackers', 'Team with full product and demo artifacts.', 'ACTIVE');

                INSERT INTO team_members
                (team_id, user_id, member_role, status, joined_at)
                VALUES
                    (8, 13, 'LEADER', 'ACTIVE', '2026-06-11 09:00:00'),
                    (8, 15, 'MEMBER', 'ACTIVE', '2026-06-11 09:05:00'),
                    (8, 16, 'MEMBER', 'ACTIVE', '2026-06-11 09:10:00'),
                    (9, 14, 'LEADER', 'ACTIVE', '2026-06-11 10:00:00'),
                    (9, 17, 'MEMBER', 'ACTIVE', '2026-06-11 10:05:00'),
                    (9, 18, 'MEMBER', 'ACTIVE', '2026-06-11 10:10:00');

                INSERT INTO judge_assignments
                (id, judge_id, event_id, round_id, category_id, assigned_by, status)
                VALUES
                    (3, 4, 3, 3, 5, 3, 'ACTIVE'),
                    (4, 5, 3, 3, 5, 3, 'ACTIVE'),
                    (5, 4, 3, 4, 5, 3, 'ACTIVE'),
                    (6, 5, 3, 4, 5, 3, 'ACTIVE');

                INSERT INTO submissions
                (id, team_id, round_id, submitted_by, attempt_number, repo_url, demo_url, slide_url,
                 report_url, change_note, status, submitted_at, github_metadata)
                VALUES
                    (5, 8, 3, 13, 1,
                     NULL,
                     NULL,
                     'https://docs.google.com/presentation/d/pitch-masters-v1',
                     NULL,
                     'Pitch deck v1 for presentation round', 'SUBMITTED', '2026-07-01 09:00:00',
                     JSON_OBJECT('artifact_type', 'slide')),
                    (6, 8, 3, 13, 2,
                     NULL,
                     NULL,
                     'https://docs.google.com/presentation/d/pitch-masters-v2',
                     NULL,
                     'Pitch deck v2 with updated story flow', 'SUBMITTED', '2026-07-03 09:15:00',
                     JSON_OBJECT('artifact_type', 'slide')),
                    (7, 9, 3, 14, 1,
                     NULL,
                     NULL,
                     'https://docs.google.com/presentation/d/full-stackers-v1',
                     NULL,
                     'Pitch deck for comparison team', 'SUBMITTED', '2026-07-02 14:20:00',
                     JSON_OBJECT('artifact_type', 'slide')),
                    (8, 8, 4, 13, 1,
                     'https://github.com/demo/pitch-masters',
                     'https://demo.seal.local/pitch-masters',
                     'https://docs.google.com/presentation/d/pitch-masters-final',
                     'https://docs.google.com/document/d/pitch-masters-report',
                     'Final round package for promoted team', 'SUBMITTED', '2026-07-14 11:30:00',
                     JSON_OBJECT('artifact_type', 'full_package'));

                INSERT INTO rankings
                (id, event_id, round_id, category_id, team_id, total_score, rank_position, is_promoted, computed_by, snapshot_note)
                VALUES
                    (5, 3, 3, 5, 8, 91.250, 1, TRUE, 3, 'Pitch round ranking for presentation event.'),
                    (6, 3, 3, 5, 9, 84.500, 2, FALSE, 3, 'Pitch round ranking for presentation event.');

                INSERT INTO notifications
                (recipient_id, event_id, notification_type, title, message, is_read)
                VALUES
                    (13, 3, 'SUBMISSION_RECEIVED', 'Pitch Round submission received',
                     'Your slide submission for Pitch Round has been recorded.', FALSE),
                    (14, 3, 'SUBMISSION_RECEIVED', 'Pitch Round submission received',
                     'Your slide submission for Pitch Round has been recorded.', FALSE);

                -- Trao giải thưởng cho EVENT 2
-- Sử dụng Subquery tương tự cho Event 2 để đảm bảo an toàn dữ liệu
                INSERT INTO awards (event_id, team_id, ranking_id, award_type, description, awarded_by) VALUES
                                                                                                            (2, 5, (SELECT id FROM rankings WHERE event_id = 2 AND team_id = 5 LIMIT 1), 'FIRST_PLACE', 'Nhà vô địch bảng AI Computer Vision Fall 2026', 3),
(2, 6, (SELECT id FROM rankings WHERE event_id = 2 AND team_id = 6 LIMIT 1), 'BEST_TECHNICAL', 'Giải thuật toán phức tạp và công nghệ đột phá', 3)
                ON DUPLICATE KEY UPDATE description=VALUES(description);