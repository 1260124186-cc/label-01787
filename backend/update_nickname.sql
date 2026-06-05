-- 更新管理员昵称
UPDATE admin_user SET nickname = CONCAT(username, '管理员') WHERE nickname IS NULL OR nickname = '';
-- 查看结果
SELECT id, username, nickname FROM admin_user;
