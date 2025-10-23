package uk.gov.hmcts.opal.common.user.authorisation.model;

/**
 * Describes an application-specific permission that can be matched against the permissions
 * exposed in {@link UserState}.
 *
 * <p>Each service that consumes the shared authentication models supplies its own implementation,
 * typically as an {@code enum}. The only contract is that a permission exposes the numeric id used
 * by the user service (and optionally a human readable description).</p>
 */
public interface PermissionDescriptor {

    /**
     * The id persisted in and returned from the user service.
     */
    long getId();

    /**
     * Optional human-readable description.
     */
    String getDescription();
}
