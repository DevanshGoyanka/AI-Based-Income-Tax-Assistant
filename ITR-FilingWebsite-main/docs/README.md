# ITR Filing Assistant

A comprehensive Income Tax Return (ITR) filing web application for Assessment Year 2026-27, compliant with CBDT e-Filing validation rules.

## Features

- **Multi-Client Management**: Manage multiple clients with their PAN, Aadhaar, and personal details
- **ITR-1 (Sahaj) Form**: Complete implementation with all schedules
- **Dual Tax Regime**: Automatic comparison between Old Regime and New Regime (115BAC)
- **Age-Based Tax Calculation**: Automatic classification into Regular, Senior Citizen, and Super Senior Citizen
- **House Property Loss Carry Forward**: Automatic import of HP loss from previous years
- **Prefill JSON Import**: Import data from Income Tax Department's prefill JSON
- **PDF/Excel/JSON Export**: Download computation reports in multiple formats
- **Real-time Validation**: CBDT-compliant validation rules
- **Secure Authentication**: JWT-based authentication system

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.2.3
- PostgreSQL (with H2 for development)
- Maven
- JWT Authentication
- Flyway for database migrations

### Frontend
- Next.js 14
- React 18
- TypeScript
- Tailwind CSS
- Axios for API calls

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- PostgreSQL 14 or higher (or use H2 for development)
- Maven 3.8+

## Installation

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Configure database in `application.properties` or use H2 (default)

3. Run the application:
```bash
./mvnw spring-boot:run
```

Backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies:
```bash
npm install
```

3. Create `.env.local` file:
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

4. Run the development server:
```bash
npm run dev
```

Frontend will start on `http://localhost:3000`

## Default Login Credentials

- Email: `user1@itr.com`
- Password: `password123`

## Tax Calculation Features

### Assessment Year 2026-27 (FY 2025-26)

#### New Regime (115BAC) - Budget 2025/2026
- ₹0 - ₹4L: 0%
- ₹4L - ₹8L: 5%
- ₹8L - ₹12L: 10%
- ₹12L - ₹16L: 15%
- ₹16L - ₹20L: 20%
- ₹20L - ₹24L: 25%
- Above ₹24L: 30%
- Standard Deduction: ₹75,000
- Rebate u/s 87A: Up to ₹12L income (max ₹60,000)

#### Old Regime
- ₹0 - ₹2.5L: 0% (Regular)
- ₹0 - ₹3L: 0% (Senior Citizen, 60-79 years)
- ₹0 - ₹5L: 0% (Super Senior Citizen, 80+ years)
- ₹2.5L/3L/5L - ₹5L: 5%
- ₹5L - ₹10L: 20%
- Above ₹10L: 30%
- Standard Deduction: ₹50,000
- Rebate u/s 87A: Up to ₹5L income (max ₹12,500)

### Special Features

- **Section 288A Rounding**: Proper rounding logic (0=no change, 1-4=down, 5-9=up)
- **HP Loss Set-off Cap**: ₹2,00,000 per Section 71(3A)
- **HP Loss Carry Forward**: Automatic import for 8 years per Section 71B
- **Surcharge Calculation**: Progressive rates based on income slabs
- **Health & Education Cess**: 4% on (tax + surcharge)
- **Balance Tax Threshold**: ₹10 minimum per CBDT practice

## Project Structure

```
ITR-FilingWebsite-main/
├── backend/
│   ├── src/main/java/com/itr/
│   │   ├── config/          # Security & app configuration
│   │   ├── controller/      # REST API endpoints
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Exception handlers
│   │   ├── mapper/          # JSON builders
│   │   ├── model/           # Domain models
│   │   ├── repository/      # Data access layer
│   │   ├── security/        # JWT authentication
│   │   └── service/         # Business logic
│   └── src/main/resources/
│       └── db/migration/    # Flyway migrations
├── frontend/
│   ├── src/
│   │   ├── app/             # Next.js pages
│   │   ├── components/      # React components
│   │   └── lib/             # API client & utilities
│   └── public/              # Static assets
└── README.md
```

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration

### Clients
- `GET /api/clients` - Get all clients
- `POST /api/clients` - Create client
- `GET /api/clients/{id}` - Get client details
- `PUT /api/clients/{id}` - Update client
- `DELETE /api/clients/{id}` - Delete client

### ITR-1 Form
- `GET /api/itr1/{clientId}/{year}` - Get form data
- `POST /api/itr1/{clientId}/{year}/save` - Save form
- `POST /api/itr1/{clientId}/{year}/compute` - Compute tax
- `GET /api/itr1/{clientId}/{year}/pdf` - Download PDF
- `GET /api/itr1/{clientId}/{year}/excel` - Download Excel
- `GET /api/itr1/{clientId}/{year}/json` - Download JSON

### Prefill
- `POST /api/clients/{clientId}/prefill/{year}` - Upload prefill JSON

## Contributing

This is a commercial project. Please contact the repository owner for contribution guidelines.

## License

Proprietary - All rights reserved

## Support

For support, please contact the development team.

---

**Note**: This application is designed for Assessment Year 2026-27 (FY 2025-26) and implements CBDT e-Filing validation rules as per the latest guidelines.
