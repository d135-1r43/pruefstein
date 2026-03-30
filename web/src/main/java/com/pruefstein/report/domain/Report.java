package com.pruefstein.report.domain;

import java.time.Instant;
import java.util.List;

import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.user.domain.AppUser;
import io.quarkus.hibernate.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Report extends PanacheEntity
{
	private String deviceId;

	private String userId;

	private Instant checkedAt;

	@ManyToOne
	private AppUser appUser;

	@OneToMany(mappedBy = "report", cascade = CascadeType.ALL)
	private List<ComplianceResult> results;

	public String getDeviceId()
	{
		return deviceId;
	}

	public void setDeviceId(String deviceId)
	{
		this.deviceId = deviceId;
	}

	public String getUserId()
	{
		return userId;
	}

	public void setUserId(String userId)
	{
		this.userId = userId;
	}

	public Instant getCheckedAt()
	{
		return checkedAt;
	}

	public void setCheckedAt(Instant checkedAt)
	{
		this.checkedAt = checkedAt;
	}

	public AppUser getAppUser()
	{
		return appUser;
	}

	public void setAppUser(AppUser appUser)
	{
		this.appUser = appUser;
	}

	public List<ComplianceResult> getResults()
	{
		return results;
	}
}
