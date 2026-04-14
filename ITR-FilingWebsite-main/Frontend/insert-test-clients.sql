-- Insert 10 test clients for ITR-1 through ITR-4
-- Run this in your PostgreSQL database

INSERT INTO clients (pan, name, email, mobile, aadhaar, dob, user_id, created_at, updated_at) VALUES
('ABCDE1234F', 'Rajesh Kumar', 'rajesh.kumar@example.com', '+91 9876543210', '123456789012', '1985-03-15', 3, NOW(), NOW()),
('XYZAB5678C', 'Priya Sharma', 'priya.sharma@example.com', '+91 9876543211', '234567890123', '1990-07-22', 3, NOW(), NOW()),
('PQRST9012G', 'Amit Patel', 'amit.patel@example.com', '+91 9876543212', '345678901234', '1982-11-08', 3, NOW(), NOW()),
('LMNOP3456H', 'Neha Gupta', 'neha.gupta@example.com', '+91 9876543213', '456789012345', '1988-05-30', 3, NOW(), NOW()),
('DEFGH7890I', 'Sanjay Singh', 'sanjay.singh@example.com', '+91 9876543214', '567890123456', '1975-09-12', 3, NOW(), NOW()),
('JKLMN2345J', 'Kavita Verma', 'kavita.verma@example.com', '+91 9876543215', '678901234567', '1992-01-25', 3, NOW(), NOW()),
('STUVW6789K', 'Vikram Reddy', 'vikram.reddy@example.com', '+91 9876543216', '789012345678', '1980-06-18', 3, NOW(), NOW()),
('BCDEF0123L', 'Anjali Nair', 'anjali.nair@example.com', '+91 9876543217', '890123456789', '1995-12-03', 3, NOW(), NOW()),
('GHIJK4567M', 'Rahul Mehta', 'rahul.mehta@example.com', '+91 9876543218', '901234567890', '1987-04-27', 3, NOW(), NOW()),
('NOPQR8901N', 'Pooja Joshi', 'pooja.joshi@example.com', '+91 9876543219', '012345678901', '1993-08-14', 3, NOW(), NOW());

-- Verify insertion
SELECT id, pan, name, email FROM clients WHERE user_id = 3 ORDER BY id DESC LIMIT 10;
