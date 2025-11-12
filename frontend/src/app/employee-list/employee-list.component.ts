import { Component, OnInit } from '@angular/core';
import { Employee } from '../employee';
import { EmployeeService } from '../employee.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-employee-list',
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.css']
})
export class EmployeeListComponent implements OnInit {

  employees: Employee[] = [];
  EnteredID!: number;

  // local file holder per employee id (map employeeId -> File)
  resumeFiles: { [empId: number]: File | undefined } = {};

  // job id input used for re-scoring
  jobIdForScoring?: number;

  loading = false;

  constructor(private employeeService: EmployeeService, private router: Router) {}

  ngOnInit(): void {
    this.getEmployees();
  }

  goToEmployee() {
    if (!this.EnteredID) return;
    this.router.navigate(['details-of-employee', this.EnteredID]);
  }

  getEmployees() {
    this.loading = true;
    this.employeeService.getEmployeesList().subscribe({
      next: data => { this.employees = data; this.loading = false; },
      error: err => { console.error(err); this.loading = false; }
    });
  }

  updateEmployee(id: number) {
    this.router.navigate(['updating-by-id', id]);
  }

  deleteEmployee(id: number) {
    if (confirm("Are you sure to delete Employee ID: " + id)) {
      this.employeeService.deleteEmployee(id).subscribe({
        next: () => this.getEmployees(),
        error: err => console.error(err)
      });
    }
  }

  detailsOfEmployee(id: number) {
    this.router.navigate(['details-of-employee', id]);
  }

  // -------------------------
  // Resume upload handling
  // -------------------------
  onResumeSelected(event: Event, empId: number) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length) {
      this.resumeFiles[empId] = input.files[0];
    }
  }

  uploadResume(empId: number) {
    const file = this.resumeFiles[empId];
    if (!file) {
      alert('Please select a resume file for upload.');
      return;
    }
    // optional: pass jobIdForScoring to backend so it can score immediately
    const jobId = this.jobIdForScoring;
    this.employeeService.uploadResume(empId, file, jobId).subscribe({
      next: (resp) => {
        alert('Resume uploaded — parsing/shortlisting started.');
        // refresh list after upload to show updated score/status (backend-dependent)
        this.getEmployees();
        // clear selected file
        delete this.resumeFiles[empId];
      },
      error: (err) => {
        console.error(err);
        alert('Upload failed: ' + (err?.message || err));
      }
    });
  }

  // -------------------------
  // Re-score (shortlist) for a job using ShortlistController endpoint
  // -------------------------
  reScore(emp: Employee) {
    if (!this.jobIdForScoring) {
      alert('Please enter Job ID for scoring above (Job ID field).');
      return;
    }
    const jobId = this.jobIdForScoring;
    // EmployeeService should provide reScoreForJob(jobId, employeeId)
    this.employeeService.reScoreForJob(jobId, emp.id!).subscribe({
      next: (res) => {
        alert('Re-scoring finished — refreshing list.');
        this.getEmployees();
      },
      error: (err) => {
        console.error(err);
        alert('Re-score failed: ' + (err?.message || err));
      }
    });
  }
}
