package de.retest.web.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

import de.retest.recheck.Recheck;
import de.retest.recheck.RecheckImpl;
import de.retest.web.testutils.PageFactory;
import de.retest.web.testutils.WebDriverFactory;
import de.retest.web.testutils.WebDriverFactory.Driver;

class SimplePageDiffIT {

	WebDriver driver;
	Recheck re;

	@BeforeEach
	public void setup() {
		re = new RecheckImpl();
		driver = WebDriverFactory.driver( Driver.CHROME );
	}

	@Disabled( "Unignore as soon as TODO from getAllElementsByPath.js is addressed." )
	@Test
	public void testSimpleChange() throws Exception {
		re.startTest();

		// Generate golden master using simple-page.html
		driver.get( PageFactory.toPageUrlString( "simple-page-diff.html" ) );

		re.check( driver, "open" );
		try {
			re.capTest();
			fail( "Assertion Error expected" );
		} catch ( final AssertionError e ) {
			assertThat( e ).hasMessageContaining( "Test 'testSimpleChange' has 4 difference(s) in 1 state(s):" ) //
					.hasMessageEndingWith( "\tdiv (div-5f5f0) at 'html[1]/body[1]/div[3]':\n" //
							+ "\t\tid:\n" //
							+ "\t\t  expected=\"twoblocks\",\n" //
							+ "\t\t    actual=\"changedblock\"\n" //
							+ "\tp (some_text) at 'html[1]/body[1]/div[3]/p[1]':\n" //
							+ "\t\ttext:\n" //
							+ "\t\t  expected=\"Some text\",\n" //
							+ "\t\t    actual=\"Some changed text\"\n" //
							+ "\tp (some_more_text) at 'html[1]/body[1]/div[3]/p[2]':\n" //
							+ "\t\twas deleted\n" //
							+ "\th2 (subheading) at 'html[1]/body[1]/h2[1]':\n" //
							+ "\t\twas inserted" );
		}
	}

	@AfterEach
	public void tearDown() {
		driver.quit();
		re.cap();
	}
}
