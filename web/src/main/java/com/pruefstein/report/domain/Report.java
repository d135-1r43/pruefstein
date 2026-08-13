package com.pruefstein.report.domain;

import java.time.Instant;
import java.util.List;

import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.user.domain.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Report extends PanacheEntity
{
	private String deviceId;

	private String userId;

	private Instant checkedAt;

	@Enumerated(EnumType.STRING)
	private ReportStatus status = ReportStatus.COMPLIANT;

	private Instant deadline;

	private Instant finalizedAt;

	/**
	 * Set once the pre-deadline reminder mail went out, so it is sent only
	 * once.
	 */
	private Instant reminderSentAt;

	@Column(length = 64)
	private String flowInstanceId;

	private String keycloakUser;

	@ManyToOne
	private AppUser appUser;

	@OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
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

	public ReportStatus getStatus()
	{
		return status;
	}

	public void setStatus(ReportStatus status)
	{
		this.status = status;
	}

	public Instant getDeadline()
	{
		return deadline;
	}

	public void setDeadline(Instant deadline)
	{
		this.deadline = deadline;
	}

	public Instant getFinalizedAt()
	{
		return finalizedAt;
	}

	public void setFinalizedAt(Instant finalizedAt)
	{
		this.finalizedAt = finalizedAt;
	}

	public Instant getReminderSentAt()
	{
		return reminderSentAt;
	}

	public void setReminderSentAt(Instant reminderSentAt)
	{
		this.reminderSentAt = reminderSentAt;
	}

	public String getFlowInstanceId()
	{
		return flowInstanceId;
	}

	public void setFlowInstanceId(String flowInstanceId)
	{
		this.flowInstanceId = flowInstanceId;
	}

	public String getKeycloakUser()
	{
		return keycloakUser;
	}

	public void setKeycloakUser(String keycloakUser)
	{
		this.keycloakUser = keycloakUser;
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
