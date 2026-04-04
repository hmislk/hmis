# Channel / Booking API Documentation

Base path: `/api/channel`
Authentication: `Token` HTTP header (the booking agent's API key)

All endpoints return JSON. Success responses use HTTP 202 with `"code": "202"`.
Error responses use HTTP 4xx with `"code": 406` and a `"message"` field.

---

## 1. List Specializations

**POST** `/api/channel/specializations`

Returns all medical specialties available for online booking.

### Request Body
```json
{
  "type": "any_string",
  "bookingChannel": "AGENT_CODE"
}
```

| Field          | Type   | Required | Notes                                    |
|----------------|--------|----------|------------------------------------------|
| type           | String | Yes      | Arbitrary string (reserved for future use) |
| bookingChannel | String | Yes      | Booking agent/channel code               |

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "specialityMap": {
      "1": "Cardiology",
      "2": "Neurology"
    }
  }
}
```
`specialityMap` keys are speciality IDs (as strings), values are speciality names.

---

## 2. List Hospitals

**POST** `/api/channel/hospitals`

Returns all hospitals/channeling centers available for booking.

### Request Body
```json
{
  "type": "any_string",
  "bookingChannel": "AGENT_CODE"
}
```

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "hosMap": [
      {
        "hospitalName": "City Hospital",
        "hospitalId": "10",
        "hospitalCode": "CITY",
        "hospitalCity": "Colombo"
      }
    ]
  },
  "detailMessage": "Success"
}
```

---

## 3. List Available Doctors

**POST** `/api/channel/doctors`

Returns all consultants registered in the system with their hospital and specialty details.

### Request Body
```json
{
  "type": "any_string",
  "bookingChannel": "AGENT_CODE"
}
```

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "resultMap": {
      "42": {
        "DoctorId": "42",
        "DocName": "Dr. A. Perera",
        "SpecName": "Cardiology",
        "SpecializationId": "1",
        "HosName": "City Hospital",
        "HosId": "10",
        "HosCode": "CITY",
        "HosTown": "Colombo",
        "DoctorNotes": ""
      }
    }
  },
  "detailMessage": "Success"
}
```
`resultMap` is keyed by doctor ID.

---

## 4. Check Doctor Availability

**POST** `/api/channel/doctorAvailability`

Lists session instances matching optional filters (hospital, specialty, doctor name, date).

### Request Body
```json
{
  "type": "any_string",
  "bookingChannel": "AGENT_CODE",
  "hosID": "10",
  "specID": "1",
  "date": "25-12-2025",
  "name": "Perera"
}
```

| Field          | Type   | Required | Notes                                              |
|----------------|--------|----------|----------------------------------------------------|
| type           | String | No       | Arbitrary string                                   |
| bookingChannel | String | Yes      | Booking agent code                                 |
| hosID          | String | No       | Hospital ID (from `/hospitals`)                    |
| specID         | String | No       | Speciality ID (from `/specializations`)            |
| date           | String | No       | Date in `dd-MM-yyyy` format                        |
| name           | String | No       | Doctor name (partial match)                        |

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "resultMap": [
      {
        "DoctorNo": "42",
        "DocName": "Dr. A. Perera",
        "SpecName": "Cardiology",
        "SpecializationId": "1",
        "HosName": "City Hospital",
        "HosCode": "CITY",
        "HosTown": "Colombo",
        "AppDate": "25 Dec 2025",
        "AppDay": "Thu",
        "SessionStart": "09:00:00"
      }
    ]
  },
  "detailMessage": "Success"
}
```

---

## 5. Search Doctors (with Sessions)

**GET** `/api/channel/searchData`

Search for doctors by name or number with optional date and hospital filters.

### Query Parameters

| Parameter   | Type    | Required         | Notes                          |
|-------------|---------|------------------|--------------------------------|
| hosID       | Integer | No               | Hospital ID                    |
| docNo       | Integer | Conditional      | Doctor ID (required if no docName) |
| docName     | String  | Conditional      | Doctor name (required if no docNo) |
| specID      | Integer | No               | Speciality ID                  |
| offset      | Integer | No               | Pagination offset               |
| page        | Integer | No               | Page number                     |
| sessionDate | String  | No               | Date in `dd-MM-yyyy` format    |

At least one of `docNo` or `docName` is required.

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": [
    {
      "sessionID": 5001,
      "docName": "Dr. A. Perera",
      "docNo": 42,
      "appDate": "2025-12-25",
      "appDay": "Thu",
      "startTime": "09:00:00",
      "hosName": "City Hospital",
      "hosId": "10",
      "specID": "1",
      "amount": 1500.00,
      "maxPatient": 30,
      "status": "Available"
    }
  ]
}
```

---

## 6. Get Doctor Sessions

**POST** `/api/channel/doctorSessions`

Returns upcoming session instances for a specific doctor and hospital.

### Request Body
```json
{
  "hosID": "10",
  "docNo": "42",
  "bookingChannel": "AGENT_CODE"
}
```

| Field          | Type   | Required | Notes                  |
|----------------|--------|----------|------------------------|
| hosID          | String | Yes      | Hospital ID            |
| docNo          | String | No       | Doctor ID              |
| bookingChannel | String | Yes      | Booking agent code     |

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "result": [
      {
        "sessionID": 5001,
        "docName": "Dr. A. Perera",
        "docNo": 42,
        "hosName": "City Hospital",
        "hosId": "10",
        "specID": "1",
        "appDate": "2025-12-25",
        "appDay": "Thu",
        "startTime": "09:00:00",
        "amount": 1500.00,
        "foreignAmount": 3000.00,
        "maxPatient": 30,
        "activePatient": 10,
        "nextNo": 11,
        "docFee": 1200.00,
        "hosFee": 300.00,
        "status": "Available",
        "remarks": ""
      }
    ]
  },
  "page": { "pageNo": 0, "offset": 0, "pages": 0 },
  "detailMessage": "Success"
}
```

`status` values: `Available`, `Full`, `Hold`, `Holiday`

---

## 7. Get Single Doctor Session

**POST** `/api/channel/doctorSession`

Returns full details of one session instance by ID, including availability and fee breakdown.

### Request Body
```json
{
  "sessionID": "5001",
  "bookingChannel": "AGENT_CODE"
}
```

| Field          | Type   | Required | Notes                        |
|----------------|--------|----------|------------------------------|
| sessionID      | String | Yes      | Session instance ID          |
| bookingChannel | String | Yes      | Booking agent code           |

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "result": {
      "sessionID": 5001,
      "docName": "Dr. A. Perera",
      "docNo": "42",
      "hosName": "City Hospital",
      "hosId": "10",
      "specID": "1",
      "appDate": "2025-12-25",
      "appDay": "Thu",
      "startTime": "09:00:00",
      "amount": 1500.00,
      "foreignAmount": 3000.00,
      "maxPatient": 30,
      "activePatient": 10,
      "nextNo": 11,
      "docFee": 1200.00,
      "hosFee": 300.00,
      "docForeignFee": 2500.00,
      "hosForeignFee": 500.00,
      "status": "Available",
      "remarks": "Session will have on time"
    }
  },
  "detailMessage": "Success"
}
```

---

## 8. Create Booking (Save)

**POST** `/api/channel/save`

Creates a **temporary (unpaid) booking** for a patient in a session. The booking is reserved but not finalized until `/complete` is called.

> **Warning:** Wrong parameters (wrong sessionID, duplicate refNo) can result in bad appointments. Always confirm session availability via `/doctorSession` first.

### Request Body
```json
{
  "sessionID": "5001",
  "patient": {
    "patientName": "John Silva",
    "title": "Mr",
    "teleNo": "0771234567",
    "nid": "199012345678",
    "foreign": "0",
    "clientRefNumber": "AGT-REF-001",
    "email": "john@example.com",
    "address": "123 Main St, Colombo"
  },
  "payment": {
    "paymentChannel": "AGENT_CODE",
    "paymentMode": "Agent Name",
    "channelFrom": "Web"
  }
}
```

| Field                    | Type   | Required | Notes                                          |
|--------------------------|--------|----------|------------------------------------------------|
| sessionID                | String | Yes      | Session instance ID                            |
| patient.patientName      | String | Yes      | Patient's full name                            |
| patient.title            | String | Yes      | Title: `Mr`, `Mrs`, `Miss`, `Dr`, etc.         |
| patient.teleNo           | String | Yes      | Phone number (minimum 10 digits)               |
| patient.nid              | String | No       | National ID / passport number                  |
| patient.foreign          | String | Yes      | `"0"` = local, `"1"` = foreigner               |
| patient.clientRefNumber  | String | Yes      | Unique reference number from booking agent     |
| patient.email            | String | No       | Patient email                                  |
| patient.address          | String | No       | Patient address                                |
| payment.paymentChannel   | String | Yes      | Booking agent code                             |
| payment.paymentMode      | String | No       | Booking agent name                             |
| payment.channelFrom      | String | No       | Booking source description (e.g., "Web")       |

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "refNo": "AGT-REF-001",
    "patientNo": 11,
    "allPatientNo": 12,
    "status": "unpaid",
    "showPno": true,
    "showTime": true,
    "chRoom": "Room 1",
    "patient": { ... },
    "sessionDetails": { ... },
    "price": { ... },
    "payment": { ... }
  }
}
```

---

## 9. Edit Booking

**POST** `/api/channel/edit`

Updates patient details for an existing **unpaid/temporary** booking identified by reference number.

### Request Body
```json
{
  "refNo": "AGT-REF-001",
  "patient": {
    "patientName": "John Silva",
    "title": "Mr",
    "teleNo": "0771234567",
    "nid": "199012345678",
    "email": "john@example.com",
    "address": "123 Main St"
  },
  "payment": {
    "paymentChannel": "AGENT_CODE",
    "paymentMode": "Agent Name"
  }
}
```

| Field                 | Type   | Required | Notes                               |
|-----------------------|--------|----------|-------------------------------------|
| refNo                 | String | Yes      | Client reference number             |
| patient.patientName   | String | Yes      | Updated patient name                |
| patient.title         | String | Yes      | Updated title                       |
| patient.teleNo        | String | Yes      | Updated phone (min 10 digits)       |
| patient.nid           | String | Yes      | Updated NID                         |
| patient.email         | String | No       | Updated email                       |
| patient.address       | String | No       | Updated address                     |
| payment.paymentChannel| String | Yes      | Booking agent code                  |
| payment.paymentMode   | String | No       | Booking agent name                  |

### Response
Same structure as `/save` response with `"status": "unpaid"`.

---

## 10. Complete Booking

**POST** `/api/channel/complete`

Finalizes a temporary booking, converting it to a paid confirmed appointment.

> **Warning:** This is a financial transaction. Use the exact `refNo` from `/save` and confirm the `price` amount matches expectations before calling.

### Request Body
```json
{
  "refNo": "AGT-REF-001",
  "statusId": 1,
  "price": "1500.00",
  "payment": {
    "paymentChannel": "AGENT_CODE",
    "paymentMode": "Agent Name"
  }
}
```

| Field                 | Type   | Required | Notes                                     |
|-----------------------|--------|----------|-------------------------------------------|
| refNo                 | String | Yes      | Client reference number from `/save`      |
| statusId              | Integer| Yes      | Must be `1` (success); `0` = failure      |
| price                 | String | Yes      | Amount paid as a string (e.g., `"1500.00"`) |
| payment.paymentChannel| String | Yes      | Booking agent code                        |
| payment.paymentMode   | String | No       | Booking agent name                        |

### Response
```json
{
  "code": "202",
  "message": "Booking completed",
  "data": {
    "refNo": "AGT-REF-001",
    "patientNo": 11,
    "allPatientNo": 12,
    "status": "ACTIVE",
    "sessionDetails": { ... },
    "patient": { ... },
    "price": { ... },
    "payment": { ... }
  },
  "detailMessage": "Your booking is setted"
}
```

---

## 11. Booking History List

**POST** `/api/channel/channelHistoryList`

Returns all bookings for a booking agent within a date range.

### Request Body
```json
{
  "fromDate": "01-12-2025",
  "toDate": "31-12-2025",
  "paymentChannel": "AGENT_CODE",
  "paymentMode": "Agent Name"
}
```

| Field          | Type   | Required | Notes                           |
|----------------|--------|----------|---------------------------------|
| fromDate       | String | Yes      | Start date in `dd-MM-yyyy`      |
| toDate         | String | Yes      | End date in `dd-MM-yyyy`        |
| paymentChannel | String | Yes      | Booking agent code              |
| paymentMode    | String | No       | Booking agent name              |

### Response
```json
{
  "code": 202,
  "message": "Accepted",
  "data": [
    {
      "RefNo": "AGT-REF-001",
      "PatientName": "John Silva",
      "NicNumber": "199012345678",
      "AppointmentNumber": 11,
      "DoctorName": "Dr. A. Perera",
      "HosName": "City Hospital",
      "HosLocation": "Colombo",
      "HosTelephone": "0112345678"
    }
  ],
  "detailMessage": "All the appoinment details listed within 01-12-2025 to 31-12-2025"
}
```

---

## 12. Get Booking Details by Reference

**POST** `/api/channel/channelHistoryByRef`

Returns full details of a booking by its client reference number.

### Request Body
```json
{
  "refNo": "AGT-REF-001",
  "bookingChannel": "AGENT_CODE"
}
```

| Field          | Type   | Required | Notes               |
|----------------|--------|----------|---------------------|
| refNo          | String | Yes      | Client reference no |
| bookingChannel | String | Yes      | Booking agent code  |

### Response
```json
{
  "code": "202",
  "message": "Booking details for ref No: AGT-REF-001",
  "data": {
    "refNo": "AGT-REF-001",
    "patientNo": 11,
    "status": "Active",
    "sessionDetails": {
      "hosId": "10",
      "hosName": "City Hospital",
      "docname": "Dr. A. Perera",
      "specialization": "Cardiology",
      "theDate": "2025-12-25",
      "theDay": "Thu",
      "startTime": "09:00:00",
      "amount": 1500.00,
      "status": "Available"
    },
    "patient": { ... },
    "price": { ... },
    "payment": { ... }
  },
  "detailMessage": "Success"
}
```

Booking `status` values: `Active`, `Patient Canceled`, `Doctor Canceled`, `Completed`, `Absent`

---

## 13. Cancel Booking

**POST** `/api/channel/cancellation`

Cancels a confirmed (paid) booking. Only active bookings can be cancelled; temporary/pending bookings expire automatically.

### Request Body
```json
{
  "refNo": "AGT-REF-001",
  "payment": {
    "paymentChannel": "AGENT_CODE",
    "paymentMode": "Agent Name"
  }
}
```

| Field                 | Type   | Required | Notes               |
|-----------------------|--------|----------|---------------------|
| refNo                 | String | Yes      | Client reference no |
| payment.paymentChannel| String | Yes      | Booking agent code  |
| payment.paymentMode   | String | No       | Booking agent name  |

### Response
```json
{
  "code": "202",
  "message": "Accepted",
  "data": {
    "refNo": "AGT-REF-001",
    "patientNo": 11,
    "status": "Patient-canceled",
    "sessionDetails": { ... },
    "patient": { ... },
    "price": { ... },
    "payment": { ... }
  },
  "detailMessage": "Booking details for ref No: AGT-REF-001"
}
```

---

## Typical Booking Workflow

```
1. POST /channel/specializations     → get specialty IDs
2. POST /channel/hospitals           → get hospital IDs
3. POST /channel/doctorAvailability  → find available sessions
4. POST /channel/doctorSession       → confirm session details and nextNo
5. POST /channel/save                → create temporary booking (get refNo)
6. POST /channel/edit                → (optional) update patient details
7. POST /channel/complete            → finalize and pay (statusId=1)
```

To cancel a confirmed booking:
```
POST /channel/cancellation  → cancel by refNo
```

To review bookings:
```
POST /channel/channelHistoryList    → list by date range
POST /channel/channelHistoryByRef  → single booking by refNo
```
