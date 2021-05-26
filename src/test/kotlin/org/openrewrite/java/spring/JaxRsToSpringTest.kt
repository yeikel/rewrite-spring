package org.openrewrite.java.spring.org.openrewrite.java.spring

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.java.spring.JaxRsToSpring

class JaxRsToSpringTest : JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .classpath("jsr311-api")
            .build()

    override val recipe: Recipe
        get() = JaxRsToSpring()

    @Test
    fun jaxRsToSpring() = assertChanged(
        before = """
            import javax.ws.rs.*;

            @Path("/troopers")
            @Produces("application/json")
            public class StormtrooperResource {
            
                @Path("/{id}")
                @GET
                public Stormtrooper getTrooper(@PathParam("id") String id) {
                    return null;
                }
            
                @POST
                public Stormtrooper createTrooper(Stormtrooper trooper) {
                    return null;
                }
            
                @Path("/{id}")
                @POST
                @PUT
                public Stormtrooper updateTrooper(@PathParam("id") String id,
                                                  Stormtrooper updatedTrooper) {
                    return null;
                }
            
                @GET
                @HEAD
                public Collection<Stormtrooper> listTroopers() {
                    return null;
                }
            }
        """,
        after = """
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestMapping;

            @RequestMapping("/troopers")
            @Produces("application/json")
            public class StormtrooperResource {
            
                @GetMapping("/{id}")
                public Stormtrooper getTrooper(@PathVariable("id") String id) {
                    return null;
                }
            
                @PostMapping
                public Stormtrooper createTrooper(Stormtrooper trooper) {
                    return null;
                }
            
                @RequestMapping(value = "/{id}", method = {POST, PUT})
                public Stormtrooper updateTrooper(@PathVariable("id") String id,
                                                  Stormtrooper updatedTrooper) {
                    return null;
                }
            
                @RequestMapping(method = {HEAD, GET})
                public Collection<Stormtrooper> listTroopers() {
                    return null;
                }
            }
        """
    )
}
