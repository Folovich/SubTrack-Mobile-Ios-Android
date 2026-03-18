INSERT INTO categories (name)
VALUES
    ('Entertainment'),
    ('Productivity'),
    ('Cloud'),
    ('Education'),
    ('Finance'),
    ('Health'),
    ('Lifestyle'),
    ('Shopping')
ON CONFLICT (name) DO NOTHING;
