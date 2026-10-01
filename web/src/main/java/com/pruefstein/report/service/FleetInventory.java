package com.pruefstein.report.service;

import java.util.List;

import com.pruefstein.compliance.domain.BlockedApp;
import com.pruefstein.compliance.domain.InstalledApp;
import com.pruefstein.compliance.repository.BlockedAppRepository;
import com.pruefstein.compliance.repository.InstalledAppRepository;
import com.pruefstein.compliance.service.BlacklistMatcher;
import com.pruefstein.homebrew.service.HomebrewCatalog;
import com.pruefstein.report.repository.LatestRun;
import com.pruefstein.report.repository.ReportRepository;
import com.pruefstein.report.service.ReportDetails.InventoryRow;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Every application installed anywhere, one row per application rather than per
 * device, so an admin can screen the estate without opening each report.
 * <p>
 * Read from each device's newest run only: an app removed since an older run is
 * not installed any more, and counting it would keep it on the list forever.
 * Scoped by {@link ReportAccess} like the reports themselves — an admin sees
 * the fleet, anybody else only their own devices.
 */
@RequestScoped
public class FleetInventory
{
	@Inject
	ReportAccess reportAccess;

	@Inject
	ReportRepository reportRepository;

	@Inject
	InstalledAppRepository installedAppRepository;

	@Inject
	BlockedAppRepository blockedAppRepository;

	@Inject
	BlacklistMatcher blacklistMatcher;

	@Inject
	HomebrewCatalog homebrewCatalog;

	/** One application and how many devices currently have it. */
	public record FleetApp(InventoryRow row, long devices)
	{
		/**
		 * Names the application across requests — what the AI analysis answers
		 * with, and what the page finds its row by.
		 */
		public String getKey()
		{
			return row.getSource() + "|" + row.getName() + "|" + blank(row.getIdentifier());
		}

		private static String blank(String value)
		{
			return value == null ? "" : value;
		}
	}

	public List<FleetApp> list()
	{
		List<Long> reportIds = reportRepository.findLatestPerDevice(reportAccess.ownerFilter()).stream()
			.map(LatestRun::reportId)
			.toList();
		List<BlockedApp> rules = blockedAppRepository.listEnabled();
		return installedAppRepository.summarize(reportIds).stream()
			.map(columns -> toFleetApp(columns, rules))
			.toList();
	}

	private FleetApp toFleetApp(Object[] columns, List<BlockedApp> rules)
	{
		// Transient, only so the row reads like a report's inventory row
		InstalledApp app = new InstalledApp();
		app.setSource((String)columns[0]);
		app.setName((String)columns[1]);
		app.setIdentifier((String)columns[2]);
		app.setVersion((String)columns[3]);
		InventoryRow row = new InventoryRow(app,
			blacklistMatcher.ruleFor(app.getSource(), app.getName(), app.getIdentifier(), rules),
			homebrewCatalog.linkFor(app.getSource(), app.getName()).orElse(null));
		return new FleetApp(row, (Long)columns[4]);
	}
}
