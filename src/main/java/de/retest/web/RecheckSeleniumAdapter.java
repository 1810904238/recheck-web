package de.retest.web;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.retest.recheck.RecheckAdapter;
import de.retest.ui.DefaultValueFinder;
import de.retest.ui.descriptors.IdentifyingAttributes;
import de.retest.ui.descriptors.RootElement;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;
import ru.yandex.qatools.ashot.shooting.ViewportPastingDecorator;

public class RecheckSeleniumAdapter implements RecheckAdapter {

	private static final String GET_ALL_ELEMENTS_BY_PATH_JS_PATH = "/javascript/getAllElementsByPath.js";

	private static final Logger logger = LoggerFactory.getLogger( RecheckSeleniumAdapter.class );

	@Override
	public boolean canCheck( final Object toVerify ) {
		return toVerify instanceof WebDriver;
	}

	@Override
	public Set<RootElement> convert( final Object toVerify ) {
		final WebDriver driver = (WebDriver) toVerify;

		final List<String> attributes = AttributeProvider.getInstance().getJoinedAttributes();
		logger.info( "Retrieving {} attributes for each element.", attributes.size() );
		final JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
		@SuppressWarnings( "unchecked" )
		final Map<String, Map<String, String>> result =
				(Map<String, Map<String, String>>) jsExecutor.executeScript( getQueryJS(), attributes );

		logger.info( "Checking website {} with {} elements.", driver.getCurrentUrl(), result.size() );

		return Collections.singleton( convertToPeers( result, driver.getTitle(), createScreenshot( driver ) ) );
	}

	public String getQueryJS() {
		try {
			return IOUtils
					.toString( RecheckSeleniumAdapter.class.getResourceAsStream( GET_ALL_ELEMENTS_BY_PATH_JS_PATH ) );
		} catch ( final IOException e ) {
			throw new RuntimeException( "Exception reading '" + GET_ALL_ELEMENTS_BY_PATH_JS_PATH + "'.", e );
		}
	}

	public RootElement convertToPeers( final Map<String, Map<String, String>> data, final String title,
			final BufferedImage screenshot ) {
		RootElementPeer root = null;
		final Map<String, WebElementPeer> converted = new HashMap<>();
		for ( final Map.Entry<String, Map<String, String>> entry : sort( data ) ) {
			final String path = entry.getKey();
			logger.debug( "Found element with path {}.", path );
			final Map<String, String> webData = entry.getValue();
			final String parentPath = getParentPath( path );
			WebElementPeer peer = converted.get( path );
			assert peer == null : "List is sorted, we should not have path twice.";
			if ( parentPath == null ) {
				root = new RootElementPeer( webData, path, title, screenshot );
				peer = root;
			} else {
				peer = new WebElementPeer( webData, path, screenshot );
				final WebElementPeer parent = converted.get( parentPath );
				assert parent != null : "We sorted the map, parent should already be there!";
				parent.addChild( peer );
			}
			converted.put( path, peer );
		}
		return root.toElement();
	}

	private List<Map.Entry<String, Map<String, String>>> sort( final Map<String, Map<String, String>> data ) {
		final List<Map.Entry<String, Map<String, String>>> sorted = new ArrayList<>( data.entrySet() );
		// Sorting ensures that parents are already created.
		Collections.sort( sorted, new Comparator<Map.Entry<String, Map<String, String>>>() {
			@Override
			public int compare( final Entry<String, Map<String, String>> o1,
					final Entry<String, Map<String, String>> o2 ) {
				return o1.getKey().compareTo( o2.getKey() );
			}
		} );
		return sorted;
	}

	private BufferedImage createScreenshot( final WebDriver driver ) {
		final ShootingStrategy shootingStrategy =
				new ViewportPastingDecorator( new CustomShootingStrategy() ).withScrollTimeout( 100 );
		final AShot aShot = new AShot().shootingStrategy( shootingStrategy );
		return aShot.takeScreenshot( driver ).getImage();
	}

	static String getParentPath( final String path ) {
		final String parentPath = path.substring( 0, path.lastIndexOf( "/" ) );
		if ( parentPath.length() == 1 ) {
			return null;
		}
		return parentPath;
	}

	@Override
	public DefaultValueFinder getDefaultValueFinder() {
		return new DefaultValueFinder() {
			@Override
			public Serializable getDefaultValue( final IdentifyingAttributes comp, final String attributesKey ) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}

}