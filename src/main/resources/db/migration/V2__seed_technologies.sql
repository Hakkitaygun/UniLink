INSERT INTO technologies (name) VALUES
                                    ('Java'), ('Spring Boot'), ('Kotlin'),
                                    ('Python'), ('Django'), ('FastAPI'), ('Flask'),
                                    ('JavaScript'), ('TypeScript'), ('Node.js'), ('Express'),
                                    ('React'), ('Next.js'), ('Vue.js'), ('Angular'), ('Tailwind CSS'), ('HTML/CSS'),
                                    ('C'), ('C++'), ('C#'), ('.NET'), ('Go'), ('Rust'), ('PHP'), ('Laravel'),
                                    ('Swift'), ('Flutter'), ('React Native'),
                                    ('SQL'), ('PostgreSQL'), ('MySQL'), ('MongoDB'), ('Redis'),
                                    ('Docker'), ('Kubernetes'), ('Git'), ('Linux'), ('AWS'), ('Azure'),
                                    ('Machine Learning'), ('TensorFlow'), ('PyTorch'),
                                    ('Unity'), ('Arduino'), ('Figma')
    ON CONFLICT DO NOTHING;