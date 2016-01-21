package ch.inftec.ju.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


/**
 * Authentication related utility methods.
 * <p>
 * Authentication is based on Spring Security. The class must run in a SpringContainer
 * and the AuthenticationProvider needs to be Autowired.
 * @author Martin
 *
 */
public class AuthUtil {
	@Autowired
	private AuthenticationProvider authenticationProvider;
	
	/**
	 * Authenticates the specified user.
	 * @param username Username
	 * @param password Password
	 * @throws AuthenticationException If authentication fails
	 */
	public void authenticate(String username, String password) throws AuthenticationException {
		UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
				username, password);
		
		Authentication auth1 = this.authenticationProvider.authenticate(token);
		
		// Set the authentication to the SecurityContext
		SecurityContextHolder.getContext().setAuthentication(auth1);
	}
	
	/**
	 * Gets whether the current Thread is authenticated.
	 * @return True if we are authenticated, false otherwise
	 */
	public boolean isAuthenticated() {
		return SecurityContextHolder.getContext().getAuthentication() != null
				&& SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
	}
	
	/**
	 * Logs the currently authenticated user out (if any).
	 */
	public void logout() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}
	
	/**
	 * Checks if we have the specified roles. If we are not authenticated, false is returned.
	 * @param roles Roles to check
	 * @return True if we are authenticated and have all specified roles
	 */
	public boolean hasAllRoles(String... roles) {
		if (this.isAuthenticated()) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			
			List<String> rolesList = JuCollectionUtils.asArrayList(roles);
			
			for (String role : this.getRoles(auth.getAuthorities())) {
				rolesList.remove(role);
			}
			
			return rolesList.size() == 0;
		}
		
		return false;
	}
	
	/**
	 * Checks if we have any of the specified roles. If we are not authenticated, false is
	 * returned.
	 * @param roles Roles to check
	 * @return True if we have any of the specified roles, false otherwise. If no roles
	 * are specified, false is returned
	 */
	public boolean hasAnyRole(String... roles) {
		if (this.isAuthenticated()) {
			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			
			List<String> rolesList = JuCollectionUtils.asArrayList(roles);
			
			for (String role : this.getRoles(auth.getAuthorities())) {
				if (rolesList.contains(role)) return true;
			}
		}
		
		return false;
	}
	
	private List<String> getRoles(Collection<? extends GrantedAuthority> grantedAuthorities) {
		List<String> roles = new ArrayList<>();
		
		for (GrantedAuthority grant : grantedAuthorities) {
			roles.add(grant.getAuthority());
		}
		
		return roles;
	}
}
