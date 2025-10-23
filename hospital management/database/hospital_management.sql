

USE hospital_management;

-- For user login information (Admin, Doctor, Patient)
CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    role ENUM('admin', 'doctor', 'patient') NOT NULL
);

-- Doctors Table
CREATE TABLE doctors (
    doctor_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialization VARCHAR(100),
    username VARCHAR(50) UNIQUE,
    FOREIGN KEY (username) REFERENCES users(username)
);

-- Patients Table
CREATE TABLE patients (
    patient_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    contact VARCHAR(50),
    age INT,
    gender VARCHAR(10),
    username VARCHAR(50) UNIQUE,
    assigned_doctor_id INT,
    FOREIGN KEY (username) REFERENCES users(username),
    FOREIGN KEY (assigned_doctor_id) REFERENCES doctors(doctor_id)
);

-- Appointments Table
CREATE TABLE appointments (
    appointment_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT,
    doctor_id INT,
    appointment_date DATE,
    status VARCHAR(20) DEFAULT 'Scheduled',
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id)
);

-- Medical Records Table
CREATE TABLE medicals (
    record_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT,
    doctor_id INT,
    visit_date DATE,
    diagnosis TEXT,
    prescription TEXT,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id),
    FOREIGN KEY (doctor_id) REFERENCES doctors(doctor_id)
);

-- Bills Table
CREATE TABLE bills (
    bill_id INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT,
    amount DECIMAL(10, 2),
    bill_date DATE,
    details VARCHAR(255),
    is_paid BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (patient_id) REFERENCES patients(patient_id)
);

-- Insert some sample data to start
INSERT INTO users (username, password, role) VALUES
('admin', 'admin123', 'admin'),
('dr.smith', 'smith123', 'doctor'),
('dr.jones', 'jones123', 'doctor'),
('john.doe', 'john123', 'patient');

INSERT INTO doctors (name, specialization, username) VALUES
('Dr. Alan Smith', 'Cardiologist', 'dr.smith'),
('Dr. Emily Jones', 'Neurologist', 'dr.jones');

INSERT INTO patients (name, contact, age, gender, username, assigned_doctor_id) VALUES
('John Doe', '123-456-7890', 45, 'Male', 'john.doe', 1);