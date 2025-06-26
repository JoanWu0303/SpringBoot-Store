INSERT INTO categories (name)
VALUES ('Fruits'),
       ('Vegetables'),
       ('Dairy'),
       ('Bakery'),
       ('Beverages'),
       ('Meat'),
       ('Snacks'),
       ('Frozen Foods'),
       ('Pantry'),
       ('Personal Care');

INSERT INTO products (name, price, description, category_id)
VALUES ('Bananas', 0.59, 'Fresh ripe bananas, sold per pound.', 1),
       ('Broccoli', 1.99, 'Green broccoli crowns, fresh and organic.', 2),
       ('Whole Milk', 3.29, 'Gallon of whole milk, pasteurized and homogenized.', 3),
       ('Sourdough Bread', 4.49, 'Freshly baked sourdough loaf with a crispy crust.', 4),
       ('Orange Juice', 3.99, '1.75L bottle of 100% pure orange juice with no added sugar.', 5),
       ('Chicken Breast', 6.99, 'Boneless, skinless chicken breast, sold per pound.', 6),
       ('Potato Chips', 2.79, 'Classic salted potato chips, family size.', 7),
       ('Frozen Pizza', 5.99, '12-inch pepperoni frozen pizza, ready to bake.', 8),
       ('Spaghetti Pasta', 1.49, '1lb of dry spaghetti pasta, made with durum wheat.', 9),
       ('Toothpaste', 3.59, 'Mint-flavored fluoride toothpaste, 6 oz tube.', 10);
