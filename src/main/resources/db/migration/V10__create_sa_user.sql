INSERT INTO roles (id, name, description)
VALUES (4, 'SA', 'Super administrador')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (company_id, username, name, email, password, role_id, status)
SELECT 1,
       'mtadmin',
       'MT Admin',
       'mtadmin@friotrack.pe',
       '$2b$10$shSIe46/pw7Z0x74Ixjn1eSXkn5BhRv373h50tWeR4s8omkfHNaDW',
       4,
       'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE company_id = 1
      AND lower(username) = 'mtadmin'
);

SELECT setval(pg_get_serial_sequence('roles', 'id'), COALESCE((SELECT MAX(id) FROM roles), 1), true);
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1), true);
