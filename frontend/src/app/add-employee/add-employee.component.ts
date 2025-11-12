import { Component } from '@angular/core';
import { Employee } from '../employee';
import { EmployeeService } from '../employee.service';
import { Router } from '@angular/router';
import { NgForm } from '@angular/forms';

@Component({
  selector: 'app-add-employee',
  templateUrl: './add-employee.component.html',
  styleUrls: ['./add-employee.component.css']
})
export class AddEmployeeComponent {

  employee: Employee = new Employee();
  photoFile!: File;
  resumeFile!: File;
  jobId?: number;



  jobs = [
  { id: 1, title: 'Java Fullstack Developer' },
  { id: 2, title: '.NET Developer' },
  { id: 3, title: 'Python Developer' },
  { id: 4, title: 'DBA (Database Admin)' }
];

  constructor(
    private employeeService: EmployeeService,
    private router: Router
  ) {}

  // ✅ Photo file select handler
  onPhotoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.photoFile = input.files[0];
      console.log('📸 Selected Photo:', this.photoFile.name);
    }
  }

  // ✅ Resume file select handler
  onResumeSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.resumeFile = input.files[0];
      console.log('📎 Selected Resume:', this.resumeFile.name);
    }
  }

  // ✅ Form Submit (Main function)
 onSubmit() {
  this.employeeService.addEmployee(this.employee).subscribe({
    next: (emp) => {
      const id = (emp as any).id;

      // पहले photo upload करो
      if (this.photoFile) {
        this.employeeService.uploadPhoto(id, this.photoFile).subscribe({
          next: () => {
            console.log("✅ Photo uploaded");

            // अब resume upload करो (अगर है)
            if (this.resumeFile) {
              this.employeeService.uploadResume(id, this.resumeFile, this.jobId).subscribe({
                next: () => {
                  console.log("✅ Resume uploaded");
                  alert("✅ Employee + Photo + Resume uploaded successfully!");
                  this.goToEmployeeList();
                },
                error: (err) => {
                  console.error("❌ Resume upload failed:", err);
                  alert("⚠️ Resume upload failed!");
                  this.goToEmployeeList();
                }
              });
            } else {
              alert("✅ Employee + Photo uploaded successfully!");
              this.goToEmployeeList();
            }
          },
          error: (err) => {
            console.error("❌ Photo upload failed:", err);
            alert("⚠️ Photo upload failed!");
            this.goToEmployeeList();
          }
        });
      } else if (this.resumeFile) {
        // अगर सिर्फ resume है
        this.employeeService.uploadResume(id, this.resumeFile, this.jobId).subscribe({
          next: () => {
            alert("✅ Employee + Resume uploaded successfully!");
            this.goToEmployeeList();
          },
          error: (err) => {
            console.error("❌ Resume upload failed:", err);
            alert("⚠️ Resume upload failed!");
            this.goToEmployeeList();
          }
        });
      } else {
        // कोई file नहीं
        alert("✅ Employee added successfully (no files)");
        this.goToEmployeeList();
      }
    },
    error: (err) => {
      console.error("❌ Employee create failed:", err);
      alert("❌ Failed to add employee");
    }
  });
}


  // ✅ Redirect to Employee List
  goToEmployeeList() {
    this.router.navigate(['/show-all-employees']);
  }

  ngOnInit(): void {}
}
