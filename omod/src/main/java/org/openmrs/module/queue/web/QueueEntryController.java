/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openmrs.module.queue.api.QueueServicesWrapper;
import org.openmrs.module.queue.api.dto.QueueEntryDto;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.web.resources.parser.QueueEntrySearchCriteriaParser;
import org.openmrs.module.webservices.rest.SimpleObject;
import org.openmrs.module.webservices.rest.web.Hyperlink;
import org.openmrs.module.webservices.rest.web.RequestContext;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.RestUtil;
import org.openmrs.module.webservices.rest.web.representation.CustomRepresentation;
import org.openmrs.module.webservices.rest.web.resource.impl.BasePageableResult;
import org.openmrs.module.webservices.rest.web.resource.impl.NeedsPaging;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping(value = "/rest/" + RestConstants.VERSION_1 + "/queueentries")
public class QueueEntryController extends BaseRestController {
	
	private final QueueServicesWrapper services;
	
	private final QueueEntrySearchCriteriaParser queueEntrySearchCriteriaParser;
	
	@Autowired
	public QueueEntryController(QueueServicesWrapper services,
	    QueueEntrySearchCriteriaParser queueEntrySearchCriteriaParser) {
		this.services = services;
		this.queueEntrySearchCriteriaParser = queueEntrySearchCriteriaParser;
	}
	
	@RequestMapping(method = { RequestMethod.GET })
	@ResponseBody
	public Object getQueueEntries(HttpServletRequest request, HttpServletResponse response) {
		CustomRepresentation customRepresentation = new CustomRepresentation("uuid");
		RequestContext context = RestUtil.getRequestContext(request, response, customRepresentation);
		System.out.println(context.getRepresentation() + customRepresentation.getRepresentation());
		Map<String, String[]> parameters = context.getRequest().getParameterMap();
		QueueEntrySearchCriteria criteria = queueEntrySearchCriteriaParser.constructFromRequest(parameters);
		List<QueueEntryDto> queueEntries = services.getQueueEntryService().getRequiredQueueEntries(criteria);
		BasePageableResult<QueueEntryDto> pageableResult = new NeedsPaging<>(queueEntries, context);
		List<Object> results = new ArrayList<Object>();
		for (QueueEntryDto queueEntry : pageableResult.getPageOfResults()) {
			SimpleObject simpleObject = new SimpleObject();
			simpleObject.add("uuid", queueEntry.getUuid());
			simpleObject.add("startedAt", queueEntry.getStartedAt());
			SimpleObject patientObject = new SimpleObject();
			patientObject.add("givenName", queueEntry.getPatientGivenName());
			patientObject.add("familyName", queueEntry.getPatientFamilyName());
			patientObject.add("uuid", queueEntry.getPatientUuid());
			simpleObject.add("patient", patientObject);
			SimpleObject locationObject = new SimpleObject();
			locationObject.add("uuid", queueEntry.getLocationUuid());
			locationObject.add("locationName", queueEntry.getLocationName());
			simpleObject.add("location", locationObject);
			results.add(simpleObject);
		}
		SimpleObject ret = new SimpleObject().add("results", results);
		boolean hasMore = pageableResult.hasMoreResults();
		if (context.getStartIndex() > 0 || hasMore) {
			List<Hyperlink> links = new ArrayList<Hyperlink>();
			if (hasMore)
				links.add(context.getNextLink());
			if (context.getStartIndex() > 0)
				links.add(context.getPreviousLink());
			ret.add("links", links);
		}
		if (Boolean.valueOf(context.getParameter("totalCount"))) {
			ret.add("totalCount", pageableResult.getTotalCount());
		}
		
		return ret;
	}
	
	@Override
	public String getNamespace() {
		return "v1/queueentries";
	}
	
}
