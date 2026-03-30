package com.pruefstein.user.domain;

import com.pruefstein.report.domain.Report;
import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class AppUser extends PanacheEntity
{
	@Column(unique = true)
	private String keycloakSubject;

	private String firstname;
	private String lastname;
	private String mail;

	@OneToMany(mappedBy = "appUser")
	private List<Report> reports;

	public String getKeycloakSubject()
	{
		return keycloakSubject;
	}

	public void setKeycloakSubject(String keycloakSubject)
	{
		this.keycloakSubject = keycloakSubject;
	}

	public String getFirstname()
	{
		return firstname;
	}

	public void setFirstname(String firstname)
	{
		this.firstname = firstname;
	}

	public String getLastname()
	{
		return lastname;
	}

	public void setLastname(String lastname)
	{
		this.lastname = lastname;
	}

	public String getMail()
	{
		return mail;
	}

	public void setMail(String mail)
	{
		this.mail = mail;
	}

	public List<Report> getReports()
	{
		return reports;
	}

	public void setReports(List<Report> reports)
	{
		this.reports = reports;
	}
}
