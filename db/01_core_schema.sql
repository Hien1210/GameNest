-- ============================================================
-- GameNest — Core Database Schema
-- Bảng cốt lõi: Accounts, Games, Questions, Answers, LFGPosts, LFGMembers
-- Theo CLAUDE.md mục 6, 7, 9, 10, 11, 21
-- Chạy bằng tài khoản admin/DBA (không dùng GameNestAppLogin)
-- ============================================================

USE GameNestDB;
GO

-- ============================================================
-- 1. Accounts
-- ============================================================
CREATE TABLE dbo.Accounts (
    account_id      INT             IDENTITY(1,1)   NOT NULL,
    username        NVARCHAR(50)    NOT NULL,
    email           NVARCHAR(255)   NOT NULL,
    password_hash   NVARCHAR(255)   NOT NULL,
    display_name    NVARCHAR(100)   NULL,
    avatar_url      NVARCHAR(500)   NULL,
    status          VARCHAR(20)     NOT NULL CONSTRAINT DF_Accounts_status DEFAULT ('ACTIVE'),
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_Accounts_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(0)    NULL,

    CONSTRAINT PK_Accounts PRIMARY KEY (account_id),
    CONSTRAINT UQ_Accounts_username UNIQUE (username),
    CONSTRAINT UQ_Accounts_email UNIQUE (email),
    CONSTRAINT CK_Accounts_status CHECK (status IN ('ACTIVE', 'BANNED', 'DELETED', 'SUSPENDED'))
);
GO

-- ============================================================
-- 2. Games
-- ============================================================
CREATE TABLE dbo.Games (
    game_id         INT             IDENTITY(1,1)   NOT NULL,
    name            NVARCHAR(150)   NOT NULL,
    description     NVARCHAR(MAX)   NULL,
    cover_image_url NVARCHAR(500)   NULL,
    status          VARCHAR(20)     NOT NULL CONSTRAINT DF_Games_status DEFAULT ('ACTIVE'),
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_Games_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(0)    NULL,

    CONSTRAINT PK_Games PRIMARY KEY (game_id),
    CONSTRAINT UQ_Games_name UNIQUE (name),
    CONSTRAINT CK_Games_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);
GO

-- ============================================================
-- 3. Questions
-- ============================================================
CREATE TABLE dbo.Questions (
    question_id     INT             IDENTITY(1,1)   NOT NULL,
    account_id      INT             NOT NULL,
    game_id         INT             NOT NULL,
    title           NVARCHAR(250)   NOT NULL,
    content         NVARCHAR(MAX)   NOT NULL,
    status          VARCHAR(20)     NOT NULL CONSTRAINT DF_Questions_status DEFAULT ('ACTIVE'),
    is_deleted      BIT             NOT NULL CONSTRAINT DF_Questions_is_deleted DEFAULT (0),
    deleted_at      DATETIME2(0)    NULL,
    deleted_by      INT             NULL,
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_Questions_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(0)    NULL,

    CONSTRAINT PK_Questions PRIMARY KEY (question_id),
    CONSTRAINT FK_Questions_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_Questions_Games FOREIGN KEY (game_id) REFERENCES dbo.Games (game_id),
    CONSTRAINT FK_Questions_DeletedBy FOREIGN KEY (deleted_by) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT CK_Questions_status CHECK (status IN ('ACTIVE', 'HIDDEN', 'DELETED', 'LOCKED'))
);
GO

CREATE INDEX IX_Questions_game_id ON dbo.Questions (game_id);
CREATE INDEX IX_Questions_account_id ON dbo.Questions (account_id);
CREATE INDEX IX_Questions_status ON dbo.Questions (status);
GO

-- ============================================================
-- 4. Answers
-- ============================================================
CREATE TABLE dbo.Answers (
    answer_id       INT             IDENTITY(1,1)   NOT NULL,
    question_id     INT             NOT NULL,
    account_id      INT             NOT NULL,
    content         NVARCHAR(MAX)   NOT NULL,
    is_accepted     BIT             NOT NULL CONSTRAINT DF_Answers_is_accepted DEFAULT (0),
    status          VARCHAR(20)     NOT NULL CONSTRAINT DF_Answers_status DEFAULT ('ACTIVE'),
    is_deleted      BIT             NOT NULL CONSTRAINT DF_Answers_is_deleted DEFAULT (0),
    deleted_at      DATETIME2(0)    NULL,
    deleted_by      INT             NULL,
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_Answers_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(0)    NULL,

    CONSTRAINT PK_Answers PRIMARY KEY (answer_id),
    CONSTRAINT FK_Answers_Questions FOREIGN KEY (question_id) REFERENCES dbo.Questions (question_id),
    CONSTRAINT FK_Answers_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_Answers_DeletedBy FOREIGN KEY (deleted_by) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT CK_Answers_status CHECK (status IN ('ACTIVE', 'HIDDEN', 'DELETED'))
);
GO

CREATE INDEX IX_Answers_question_id ON dbo.Answers (question_id);
CREATE INDEX IX_Answers_account_id ON dbo.Answers (account_id);
GO

-- ============================================================
-- 5. LFGPosts (Looking For Group)
-- ============================================================
CREATE TABLE dbo.LFGPosts (
    lfg_id          INT             IDENTITY(1,1)   NOT NULL,
    account_id      INT             NOT NULL,   -- creator
    game_id         INT             NOT NULL,
    title           NVARCHAR(250)   NOT NULL,
    description     NVARCHAR(MAX)   NULL,
    game_mode       NVARCHAR(100)   NULL,
    required_rank   NVARCHAR(100)   NULL,
    region          NVARCHAR(100)   NULL,
    max_players     INT             NOT NULL,
    current_players INT             NOT NULL CONSTRAINT DF_LFGPosts_current_players DEFAULT (0),
    start_time      DATETIME2(0)    NULL,
    status          VARCHAR(20)     NOT NULL CONSTRAINT DF_LFGPosts_status DEFAULT ('OPEN'),
    created_at      DATETIME2(0)    NOT NULL CONSTRAINT DF_LFGPosts_created_at DEFAULT (SYSUTCDATETIME()),
    updated_at      DATETIME2(0)    NULL,

    CONSTRAINT PK_LFGPosts PRIMARY KEY (lfg_id),
    CONSTRAINT FK_LFGPosts_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT FK_LFGPosts_Games FOREIGN KEY (game_id) REFERENCES dbo.Games (game_id),
    CONSTRAINT CK_LFGPosts_status CHECK (status IN ('OPEN', 'FULL', 'CLOSED', 'EXPIRED', 'DELETED')),
    CONSTRAINT CK_LFGPosts_max_players CHECK (max_players > 0),
    CONSTRAINT CK_LFGPosts_current_players CHECK (current_players >= 0 AND current_players <= max_players)
);
GO

CREATE INDEX IX_LFGPosts_game_id ON dbo.LFGPosts (game_id);
CREATE INDEX IX_LFGPosts_account_id ON dbo.LFGPosts (account_id);
CREATE INDEX IX_LFGPosts_status ON dbo.LFGPosts (status);
GO

-- ============================================================
-- 6. LFGMembers (junction table — hard delete cho phép khi rời nhóm)
-- ============================================================
CREATE TABLE dbo.LFGMembers (
    lfg_member_id   INT             IDENTITY(1,1)   NOT NULL,
    lfg_id          INT             NOT NULL,
    account_id      INT             NOT NULL,
    joined_at       DATETIME2(0)    NOT NULL CONSTRAINT DF_LFGMembers_joined_at DEFAULT (SYSUTCDATETIME()),

    CONSTRAINT PK_LFGMembers PRIMARY KEY (lfg_member_id),
    CONSTRAINT FK_LFGMembers_LFGPosts FOREIGN KEY (lfg_id) REFERENCES dbo.LFGPosts (lfg_id),
    CONSTRAINT FK_LFGMembers_Accounts FOREIGN KEY (account_id) REFERENCES dbo.Accounts (account_id),
    CONSTRAINT UQ_LFGMembers_lfg_account UNIQUE (lfg_id, account_id)
);
GO

CREATE INDEX IX_LFGMembers_lfg_id ON dbo.LFGMembers (lfg_id);
CREATE INDEX IX_LFGMembers_account_id ON dbo.LFGMembers (account_id);
GO
