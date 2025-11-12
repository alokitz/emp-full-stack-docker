import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Employee } from './employee';

@Injectable({
  providedIn: 'root'
})
export class EmployeeService {

  private baseURL = "http://localhost:8080/api/v1/employees";

  constructor(private httpClient: HttpClient) {}

  // 🔹 Get All Employees
  getEmployeesList(): Observable<Employee[]> {
    return this.httpClient.get<Employee[]>(`${this.baseURL}`);
  }

  // 🔹 Add Employee
  addEmployee(employee: Employee): Observable<any> {
    return this.httpClient.post<any>(`${this.baseURL}`, employee);
  }

  // 🔹 Get Employee by ID
  getEmployeeById(id: number): Observable<Employee> {
    return this.httpClient.get<Employee>(`${this.baseURL}/${id}`);
  }

  // 🔹 Update Employee
  updateEmployee(id: number, employee: Employee): Observable<Object> {
    return this.httpClient.put(`${this.baseURL}/${id}`, employee);
  }

  // 🔹 Delete Employee
  deleteEmployee(id: number): Observable<any> {
    return this.httpClient.delete<any>(`${this.baseURL}/${id}`, { responseType: 'text' as 'json' });
  }

  // 🔹 Upload Photo
  uploadPhoto(id: number, file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    // backend returns plain text (not JSON)
    return this.httpClient.post(`${this.baseURL}/${id}/upload-photo`, formData, {
      responseType: 'text'
    });
  }

  // 🔹 Upload Resume
  uploadResume(id: number, file: File, jobId?: number): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);

    if (jobId !== undefined && jobId !== null) {
      formData.append('jobId', jobId.toString());
    }

    return this.httpClient.post(`${this.baseURL}/${id}/upload-resume`, formData, {
      responseType: 'text'
    });
  }

  // 🔹 Re-score / call AI shortlisting for a job & employee
  // Note: ShortlistController in backend is mounted under '/api' (not /api/v1)
  reScoreForJob(jobId: number, employeeId: number): Observable<any> {
    const url = `http://localhost:8080/api/shortlist/${jobId}/employee/${employeeId}`;
    return this.httpClient.post<any>(url, {});
  }

  // Optional: helper to build resume download URL
  downloadResumeUrl(id: number): string {
    return `${this.baseURL}/${id}/resume/download`;
  }
}
