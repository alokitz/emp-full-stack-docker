import { DatePipe } from '@angular/common';

export class Employee {
  id!: number;
  fname!: string;
  lname!: string;
  profilePhotoPath?: string;
  resumePath?: string;
  email!: string;
  salary!: number;
  department: string;
  designation: string;
  joiningDate!: string;

  // 🆕 AI Resume Parsing Fields
  resumeScore?: number;      // e.g., 85 (out of 100)
  resumeStatus?: string;     // e.g., "UPLOADED", "SHORTLISTED", "REJECTED"
  parsedData?: any;          // e.g., JSON object of parsed resume content
  jobId?: number;            // optional, backend might use this to map job-resume relation

  constructor() {
    this.email = "@gmail.com";
    this.salary = 0;
    this.department = "";
    this.designation = "";
    this.joiningDate = "";

    // Initialize new fields
    this.resumeScore = 0;
    this.resumeStatus = "UPLOADED";
    this.parsedData = null;
  }
}
