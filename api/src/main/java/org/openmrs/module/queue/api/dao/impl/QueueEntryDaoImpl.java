/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao.impl;

import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.AliasToBeanResultTransformer;
import org.openmrs.module.queue.api.dao.QueueEntryDao;
import org.openmrs.module.queue.api.dto.QueueEntryDto;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.springframework.beans.factory.annotation.Qualifier;

@SuppressWarnings("unchecked")
public class QueueEntryDaoImpl extends AbstractBaseQueueDaoImpl<QueueEntry> implements QueueEntryDao<QueueEntry> {
	
	public QueueEntryDaoImpl(@Qualifier("sessionFactory") SessionFactory sessionFactory) {
		super(sessionFactory);
	}
	
	@Override
	public List<QueueEntry> getQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria c = createCriteriaFromSearchCriteria(searchCriteria);
		c.addOrder(Order.desc("qe.sortWeight"));
		c.addOrder(Order.asc("qe.startedAt"));
		c.addOrder(Order.asc("qe.dateCreated"));
		c.addOrder(Order.asc("qe.queueEntryId"));
		return c.list();
	}
	
	@Override
	public List<QueueEntryDto> getRequiredQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria c = createCriteriaFromSearchCriteria(searchCriteria);
		c.add(Restrictions.eq("pname.preferred", true));
		c.addOrder(Order.desc("qe.sortWeight"));
		c.addOrder(Order.asc("qe.startedAt"));
		c.addOrder(Order.asc("qe.dateCreated"));
		c.addOrder(Order.asc("qe.queueEntryId"));
		c.setProjection(Projections.projectionList().add(Projections.property("qe.uuid"), "uuid")
		        .add(Projections.property("p.gender"), "patientGender").add(Projections.property("p.uuid"), "patientUuid")
		        .add(Projections.property("pname.givenName"), "patientGivenName")
		        .add(Projections.property("pname.middleName"), "patientMiddleName")
		        .add(Projections.property("pname.familyName"), "patientFamilyName")
		        .add(Projections.property("pname.prefix"), "patientPrefix")
		        .add(Projections.property("l.name"), "locationName").add(Projections.property("l.uuid"), "locationUuid")
		        .add(Projections.property("qe.startedAt"), "startedAt"));
		c.setResultTransformer(new AliasToBeanResultTransformer(QueueEntryDto.class));
		return c.list();
	}
	
	@Override
	public Long getCountOfQueueEntries(QueueEntrySearchCriteria searchCriteria) {
		Criteria criteria = createCriteriaFromSearchCriteria(searchCriteria);
		criteria.setProjection(Projections.rowCount());
		return (Long) criteria.uniqueResult();
	}
	
	/**
	 * Convert the given {@link QueueEntrySearchCriteria} into ORM criteria
	 */
	private Criteria createCriteriaFromSearchCriteria(QueueEntrySearchCriteria searchCriteria) {
		Criteria c = getCurrentSession().createCriteria(QueueEntry.class, "qe");
		c.createAlias("queue", "q");
		c.createAlias("patient", "p");
		c.createAlias("p.names", "pname");
		c.createAlias("q.location", "l");
		includeVoidedObjects(c, searchCriteria.isIncludedVoided());
		limitByCollectionProperty(c, "queue", searchCriteria.getQueues());
		limitByCollectionProperty(c, "q.location", searchCriteria.getLocations());
		limitByCollectionProperty(c, "q.service", searchCriteria.getServices());
		limitToEqualsProperty(c, "qe.patient", searchCriteria.getPatient());
		limitToEqualsProperty(c, "qe.visit", searchCriteria.getVisit());
		limitByCollectionProperty(c, "qe.priority", searchCriteria.getPriorities());
		limitByCollectionProperty(c, "qe.status", searchCriteria.getStatuses());
		limitByCollectionProperty(c, "qe.locationWaitingFor", searchCriteria.getLocationsWaitingFor());
		limitByCollectionProperty(c, "qe.providerWaitingFor", searchCriteria.getProvidersWaitingFor());
		limitByCollectionProperty(c, "qe.queueComingFrom", searchCriteria.getQueuesComingFrom());
		limitToGreaterThanOrEqualToProperty(c, "qe.startedAt", searchCriteria.getStartedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "qe.startedAt", searchCriteria.getStartedOnOrBefore());
		limitToEqualsProperty(c, "qe.startedAt", searchCriteria.getStartedOn());
		limitToGreaterThanOrEqualToProperty(c, "qe.endedAt", searchCriteria.getEndedOnOrAfter());
		limitToLessThanOrEqualToProperty(c, "qe.endedAt", searchCriteria.getEndedOnOrBefore());
		limitToEqualsProperty(c, "qe.endedAt", searchCriteria.getEndedOn());
		if (searchCriteria.getHasVisit() == Boolean.TRUE) {
			c.add(Restrictions.isNotNull("qe.visit"));
		} else if (searchCriteria.getHasVisit() == Boolean.FALSE) {
			c.add(Restrictions.isNull("qe.visit"));
		}
		if (searchCriteria.getIsEnded() == Boolean.TRUE) {
			c.add(Restrictions.isNotNull("qe.endedAt"));
		} else if (searchCriteria.getIsEnded() == Boolean.FALSE) {
			c.add(Restrictions.isNull("qe.endedAt"));
		}
		return c;
	}
}
