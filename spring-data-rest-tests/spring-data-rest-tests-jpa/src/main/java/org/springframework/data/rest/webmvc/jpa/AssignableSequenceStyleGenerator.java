package org.springframework.data.rest.webmvc.jpa;

import org.hibernate.id.enhanced.SequenceStyleGenerator;

/**
 * Extension to {@link SequenceStyleGenerator} to allow assigned identifiers.
 *
 * @author Mark Paluch
 * @see <a href=
 *      "https://discourse.hibernate.org/t/manually-setting-the-identifier-results-in-object-optimistic-locking-failure-exception/10743/6">Manually
 *      setting the identifier results in object optimistic locking failure exception</a>
 * @see <a href="https://hibernate.atlassian.net/browse/HHH-17472">HHH-17472</a>
 */
public class AssignableSequenceStyleGenerator extends SequenceStyleGenerator {

	@Override
	public boolean allowAssignedIdentifiers() {
		return true;
	}
}
