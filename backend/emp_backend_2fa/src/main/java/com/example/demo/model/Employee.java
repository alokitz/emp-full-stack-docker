package com.example.demo.model;
import java.time.LocalDate;

import com.example.demo.auditable.Auditable;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;



@Entity 
@Table(name="employees_table")
public class Employee extends Auditable {

	
	public Employee() {}
	
	public Employee(String fname, String lname, String email, long salary, String department, String designation,
			LocalDate joiningDate) {
		super();
		this.fname = fname;
		this.lname = lname;
		this.email = email;
		this.salary = salary;
		this.department = department;
		this.designation = designation;
		this.joiningDate = joiningDate;
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column(name="f_name")
	private String fname;
	
	@Column(name="l_name")
	private String lname;
	
	@Column(name="mail")
	private String email;
	
	@Column(name="sal")
	private long salary;
	
	@Column(name="dep")
	private String department;
	
	@Column(name="des")
	private String designation;
	
	@Column(name="jd")
	private LocalDate joiningDate;
	
	@Column(name="profile_photo")
	private String profilePhotoPath; 
	
	@Column(name="resume")
    private String resumePath;
	
	
	
	
	
	
	
	
	
	 // Resume parsing fields
	    @Column(name="resume_parsed_text", columnDefinition = "TEXT")
	    private String resumeParsedText;
	
	    @Column(name="resume_skills")
	    private String resumeSkills;
	
	    @Column(name="resume_score")
	    private Double resumeScore;
	
	    @Column(name="resume_parsed_at")
	    private java.time.LocalDateTime resumeParsedAt;
	
	    @Column(name="resume_status")
	    private String resumeStatus;
	
	public long getId() {
		return id;
	}
	public void setId(long id) {
		this.id = id;
	}
	public String getFname() {
		return fname;
	}
	public void setFname(String fname) {
		this.fname = fname;
	}
	public String getLname() {
		return lname;
	}
	public void setLname(String lname) {
		this.lname = lname;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public long getSalary() {
		return salary;
	}
	public void setSalary(long salary) {
		this.salary = salary;
	}
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	public String getDesignation() {
		return designation;
	}
	public void setDesignation(String designation) {
		this.designation = designation;
	}
	public LocalDate getJoiningDate() {
		return joiningDate;
	}
	public void setJoiningDate(LocalDate joiningDate) {
		this.joiningDate = joiningDate;
	}

	public String getProfilePhotoPath() {
		return profilePhotoPath;
	}

	public void setProfilePhotoPath(String profilePhotoPath) {
		this.profilePhotoPath = profilePhotoPath;
	}

	public String getResumePath() {
		return resumePath;
	}

	public void setResumePath(String resumePath) {
		this.resumePath = resumePath;
	}

	public String getResumeParsedText() {
		return resumeParsedText;
	}

	public void setResumeParsedText(String resumeParsedText) {
		this.resumeParsedText = resumeParsedText;
	}

	public String getResumeSkills() {
		return resumeSkills;
	}

	public void setResumeSkills(String resumeSkills) {
		this.resumeSkills = resumeSkills;
	}

	public Double getResumeScore() {
		return resumeScore;
	}

	public void setResumeScore(Double resumeScore) {
		this.resumeScore = resumeScore;
	}

	public java.time.LocalDateTime getResumeParsedAt() {
		return resumeParsedAt;
	}

	public void setResumeParsedAt(java.time.LocalDateTime resumeParsedAt) {
		this.resumeParsedAt = resumeParsedAt;
	}

	public String getResumeStatus() {
		return resumeStatus;
	}

	public void setResumeStatus(String resumeStatus) {
		this.resumeStatus = resumeStatus;
	}

	public Long getJobId() {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}
