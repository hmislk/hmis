# CareCode Open Hospital Management Information System

## Title

Open Source Hospital Management Information System

## Description

Introducing our Hospital Information Management System, a trusted solution that has been actively serving over 40 healthcare institutions since its inception in 2004. This comprehensive system offers a suite of modules tailored to support the multifaceted workflows of contemporary hospitals. Its adaptability ensures that variances in institutional requirements are easily addressed through configuration.

From the outset, the system has consistently prioritised user-friendliness and operational speed over ornate user interfaces. In 2015, a significant evolution took place with the introduction of a multi-site architecture, further enhancing its reliability and scalability.

A cornerstone of this system is its Object-Oriented Design, which strikes a balance between simplicity and robustness. This design approach ensures flexibility, making it adept at navigating the intricate and varied business workflows inherent to the healthcare sector.

The system's architecture leans heavily on Object-Oriented Principles, consistently aligning with tried-and-true data models, such as the dm+d of NHS, UK. This alignment guarantees a design that's not only robust and optimised but also primed for seamless integration of future extensions to meet emerging needs.

Developed using Java Enterprise Edition, the system offers both a web application and a RESTful server. While the choice of database management system rests with the implementing team, MySQL or MariaDB comes highly recommended. Complementing JavaEE are technologies like JSF, JPA, and PrimeFaces. The lab middleware is crafted in C#, and standalone applications, which leverage RESTful services, are built using JavaSE.

## Current Version

Current Version: 3.0.0.20240813.1 (This line will be automatically updated to reflect the latest version)

## History

In 2004, Dr. M H B Ariyaratne, a medical doctor, pioneered the development of an Electronic Medical Record (EMR) System tailored for his general practice. Utilising Microsoft Visual Basic 6 and MS-Access, this system caught the attention of fellow doctors, leading to widespread adoption. As its user base grew, so did its features, evolving through collaborative discussions and feedback.

Recognising the diverse needs of its users, which included clinical laboratories, medical channelling centres, and hospitals, the system expanded with additional modules to cater to these requirements.

By 2012, a shift was made towards a more robust platform. A new JavaEE-based system was launched as an Open Source Initiative, hosted on GitHub under the MIT license. This transaction was supported by students from the Sri Lanka Institute of Advanced Technical Education, who contributed to the project as a part of their studies.


Commercial support was introduced to enhance the system's reach and reliability, offering services such as installations, user training, maintenance, cloud hosting, and troubleshooting. 

## Functionality
* Electronic Medical Record System / EMR
* Pharmacy Information Management System / Medical Logistics Information Management
* Laboratory Information Management System / LIMS / LIS
* Admission, Discharge and Transfer System
* Inpatient Management System
* Theatre Management System
* Appointment Management System
* OPD / Outpatient Management System
* Clinic Management System
* Human Resource Management System
* Payroll Management System
* Fleet Management System
* Linan Management System
* Inventory Management System
* Assets and Consumable Management System

### Scheduled Processes
Scheduled tasks such as stock value recordings run automatically based on
configured frequencies. The date calculations for these schedules are handled
by `ScheduledProcessService`. The `Year End` calculation now correctly sets the
next run to December 31st of the following year.


## Installation

The installation is very easy for testing. In a development setting, simply clone the project using Netbeans and configure a blank data source in the application server. Running the project will lead to an administrator page to add an institution, department and user. Production environment configurations, hardware and system requirements are discussed in detail in The wiki section.

## Usage

The features available to different roles of the users are stated in detail in the User Manual.

### Sanitizing Admin-Defined HTML

Text values saved via `setLongTextValueByKey` are cleaned with JSoup's basic
safelist before being stored. This prevents administrators from accidentally
introducing unsafe markup.

### Pharmacy Analytics Configuration

Administrators can toggle individual tabs and reports in the Pharmacy Analytics page using application options. Each tab and each command button label has a corresponding boolean configuration key. Setting a value to `false` hides that element from the UI.

Example keys include:

- `Show Pharmacy Analytics Summary Reports Tab`
- `Show Pharmacy Income Report`

## Request Originators 
* Dr M H B Ariyaratne
* Dr Jagath Samarasekara
* Dr Krishantha Widisingha
* Dr Veditha Banduwardana
* Dr Gayaman Dissanayake
* Dr Harsha Meemaduma
* Dr A T C Kumara
* Dr Saranathilaka Danthanarayana
* Dr Bolonghoge Dayanath
* Dr K M P Keerthi
* Dr Anura Krishantha

## System Architect 
* Dr M H B Ariyaratne - Initiated the Project, Product owner, System Architect, Lead Developer since 2004 to date

## Managing Director
* Mr Thisara De Silva - From 2024 to date

## Project Manager
* Mr Ramath Manjitha - From 2024 to date

## Technical Lead
* Mr Geeth Madhushan - From 2024 to date

## Business Analyst
* Miss Binuthi Nilakna Ariyaratne - From 2024 to date
  
## Developers
* Dr M H B Ariyaratne(Active)
* Mr Dushan Madhuranga (Former)
* Mr Lahiru Madhushanka (Former) 
* Mr A C M Safrin (Former)
* Mr K Pasan Anuradha (Former)
* Mr Rohan Jayasundara (Former)
* Mr Dilshan Kanishka (Former)
* Ms Ravisarani Ranawaka (Former)
* Ms Anushka De Silva (Former)
* Mr Priyanka Sandaruwan (Former)
* Mr Anura Wijesinghe (Former)
* Mr Ruwan Tharaka (Former)
* Mr Gayan Malshan (Former)
* Mr Isuru Pathum  (Former)
* Mr A R C Sanka (Former)
* Mr Senula Nanayakkara (Former)
* Mr Pavan Thiwanka Madhushan (Former)
* Mr Pasindu Wathsara (Active)
* Mr Damith Deshan (Active)
* Mr Lawan Chaamindu Jayalath Samarasekara (Active)
* Mr Chinthaka Prasad Wajerathna (Active)
* Mr Imesh Ranawella (Active)
* Mr Pubudu Piyankara (Active)
* Miss Dinuri Kithmini (Active)

## Quantity Control Feedback

* Dr A N E M Gunasekara (Former)
* Ms Nadeeka Darshani Wijesooriya (Former)
* Mr Janith Liyanage (Former)
* Ms D W C Piumini (Former)
* Ms Irani Madushika (Active) 
* Ms Deshani Pubudu (Active) 


## Credits 
Our contributors and the Open Source Community for their knowledge and time. Healthcare Institutions that provided financial supporters are listed below.

* Ruhunu MediHospital, Hambanthota  - 2012 Up to date
* Arogya Hospital, Tangalle - 2014 to 2024
* University Medical Centre, Faculty of Medicine, University of Colombo - 2025 onwards
* Kirillawela Family Healthcare Centre - 2024 onwards
* Kirinda Medical Centre - 2023 Onwards
* Sethma Hospital, Gampaha - Implementing
* Southern Lanka Hospital, Tangalle - Implementing
* Arogya Channelling Centre, Tangalle -  2010 to 2024
* Arogya Pharmacy, Tangalle -  2015 to 2024
* St. George Hospital, Biyagama -  2016 Up to 2019
* Digasiri Hospital, Puttalama - 2015 Up to date
* Suwasarana Hospital, Ragama -  2016 Up to date
* Suwani Hospital, Galle - 2015 Up to date
* Sahana Medicare, Thissamaharamaya  -  2012 Up to date
* Matara Nursing Home, Matara - 2008 - 2010
* BestLife Medical Centre, Kamburupitiya - 2008 - 2016
* New Pharmacy, Matara - 2014 Up to date
* Sri Katha Hospital, Dangedara - 2014 up to date
* Suwasahana Medical Center, Kataragama - 2015 Up to date
* Suwana Medical Center, Akuressa - 2015 Up to date
* New Multi Drugs, Galle - 2015 up to date
* Matara Pharmacy, Akuressa, 2013 up to date
* Matara Pharmacy, Deniyaya, 2013 up to date
* Matara Pharmacy, Urubokka, 2013 - 2015
* Matara Pharmacy, Galle, 2015 up to date
* Supipi Medical Centre, Kamburupitiya 2011  up to date
* Ahangama Medical Centre, Ahangama - 2008 up to date
* Galle Cooperative Hospital, Galle, Sri Lanka - 2008 up to date
* Ruhunu Hospital, Karapitiya - 2008 up to date
* Weligama Medical Center, Weligama - 2009  up to date
* Osethra Medical Centre, Beliatta - 2010 up to date
* Isuru Medihouse, Hambanthota - 2014  up to date
* Ruhunu Medihouse, Ambanlanthota - 2014  up to date
* Horizon Medical Centre, Deberawewa - 2013  up to date
* Pubudhu Medical Centre, Hakmana - 2008 - 2014
* City Lab, Matara - 2013
* Roseth Hospital, Ambalangoda - 2014 to date
* Richmond Laboratory Services, Galle - 2010 up to date
* Clinical Laboratory, Boossa - 2010 to 2020
* Baddegama Medical Services, Baddegama - 2014 up to date
* Sahana Medicare, Thissamaharamaya - 2014 up to date
* Medray Medical Laboratory, Matara - 2014 up to date
* Agunukolapelssa Medical Centre, Agunukolapelssa - 2014 up to date
* Holton Hospital, Walasmulla - 2015 up to date
* Unawatuna Medical Centre, Unawatuna, Galle - 2015 up to date
* Kegalu Medi Lab - 2014 up to date
* Raliable Medi Lab, Monaragala - 2016
* Asiri Pharmacy, Karapitiya - 2019 to date
* LifeCare Pharmacy, Thanipolgaha - 2020 to date


## License
AGPL License details are attached as the LICENSE.md

![CodeRabbit Pull Request Reviews](https://img.shields.io/coderabbit/prs/github/hmislk/hmis?utm_source=oss&utm_medium=github&utm_campaign=hmislk%2Fhmis&labelColor=171717&color=FF570A&link=https%3A%2F%2Fcoderabbit.ai&label=CodeRabbit+Reviews)
