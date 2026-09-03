package com.pruefstein.report.domain;

import java.time.Instant;
import java.util.List;

import com.pruefstein.compliance.domain.ComplianceResult;
import com.pruefstein.user.domain.AppUser;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
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

	/**
	 * When an open report runs out of time to be fixed. Set once, on the
	 * submission that opened it, and deliberately not moved by a later
	 * submission — the window belongs to the failure, not to the last attempt
	 * at fixing it.
	 */
	private Instant deadline;

	private Instant finalizedAt;

	/**
	 * Set once the pre-deadline reminder mail went out, so it is sent only
	 * once.
	 */
	private Instant reminderSentAt;

	/**
	 * Set when the outcome mail was asked for while the report's failed checks
	 * still had no explanations. The enrichment job sends it once they do — or
	 * once it has waited long enough — and clears this. {@code null} means
	 * nothing is waiting to be sent.
	 */
	private Instant mailPendingSince;

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

	public Instant getMailPendingSince()
	{
		return mailPendingSince;
	}

	public void setMailPendingSince(Instant mailPendingSince)
	{
		this.mailPendingSince = mailPendingSince;
	}

	public Instant getReminderSentAt()
	{
		return reminderSentAt;
	}

	public void setReminderSentAt(Instant reminderSentAt)
	{
		this.reminderSentAt = reminderSentAt;
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
