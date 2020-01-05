package nl.rabobank.api;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
    plugin = {"pretty"},
    features = {"classpath:feature"},
    glue = {"nl.rabobank.api.stepdefinition"})
public class CustomerResourceBehaviourIT {}
