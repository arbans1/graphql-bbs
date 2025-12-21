-- 유저 테이블
-- 추후 분리한다면 nickname / last_login_at 등은 별도 테이블로 분리(table bloat 방지), 패스워드 틀린 횟수 등 추가 고려
CREATE TABLE users (
    id VARCHAR(20) PRIMARY KEY,                    -- TSID (User.id 매핑)
    login_id VARCHAR(50) UNIQUE NOT NULL,          -- 로그인 아이디
    hashed_password VARCHAR(255) NOT NULL,         -- BCrypt 해시 비밀번호
    nickname VARCHAR(50) UNIQUE NOT NULL,          -- 닉네임
    email VARCHAR(255) UNIQUE NOT NULL,            -- 이메일
    profile_image_url VARCHAR(500),                -- 프로필 이미지
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER'     -- 권한
        CHECK (role IN ('ADMIN', 'MEMBER', 'GUEST')),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'   -- 상태 (ACTIVE, BANNED 등)
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'BANNED', 'DELETED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE
);
COMMENT ON TABLE users IS '서비스 사용자 정보 및 권한 관리 테이블';
COMMENT ON COLUMN users.id IS '사용자 고유 식별자 (TSID)';
COMMENT ON COLUMN users.login_id IS '로그인 시 사용하는 아이디';
COMMENT ON COLUMN users.hashed_password IS 'BCrypt로 암호화된 비밀번호';
COMMENT ON COLUMN users.nickname IS '사용자 닉네임';
COMMENT ON COLUMN users.role IS '권한: ADMIN(운영자), MEMBER(회원), GUEST(비회원)';
COMMENT ON COLUMN users.status IS '상태: ACTIVE(활성), SUSPENDED(정지), BANNED(차단), DELETED(탈퇴)';
COMMENT ON COLUMN users.created_at IS '사용자 생성 일시';
COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 일시';

