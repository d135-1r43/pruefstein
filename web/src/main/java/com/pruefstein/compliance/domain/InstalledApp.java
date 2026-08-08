package com.pruefstein.compliance.domain;

import com.pruefstein.report.domain.Report;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

/**
 * One application or package the device reported as installed, captured per
 * report so the report page can list the full inventory and offer to block
 * anything on it.
 * <p>
 * This is a complete record of what is installed on someone's machine. It is
 * personal data under GDPR and is very likely subject to works-council
 * agreement in Germany — treat retention and access accordingly.
 */
@Entity
public class InstalledApp extends PanacheEntity
{
	@ManyToOne(optional = false)
	private Report report;

	/** {@code app}, {@code brew:formula} or {@code brew:cask}. */
	private String source;

	private String name;

	private String identifier;

	private String version;

	@Column(columnDefinition = "TEXT")
	private String path;

	public Report getReport()
	{
		return report;
	}

	public void setReport(Report report)
	{
		this.report = report;
	}

	public String getSource()
	{
		return source;
	}

	public void setSource(String source)
	{
		this.source = source;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getIdentifier()
	{
		return identifier;
	}

	public void setIdentifier(String identifier)
	{
		this.identifier = identifier;
	}

	public String getVersion()
	{
		return version;
	}

	public void setVersion(String version)
	{
		this.version = version;
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public boolean isFromHomebrew()
	{
		return source != null && source.startsWith("brew");
	}
}
