package com.sap.psr.vulas.backend.rest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import com.sap.psr.vulas.backend.model.Bug;
import com.sap.psr.vulas.backend.model.ConstructChange;
import com.sap.psr.vulas.backend.model.ConstructChangeType;
import com.sap.psr.vulas.backend.model.ConstructId;
import com.sap.psr.vulas.backend.repo.BugRepository;
import com.sap.psr.vulas.backend.repo.ConstructChangeRepository;
import com.sap.psr.vulas.shared.enums.BugOrigin;
import com.sap.psr.vulas.shared.enums.ConstructType;
import com.sap.psr.vulas.shared.enums.ContentMaturityLevel;
import com.sap.psr.vulas.shared.enums.ProgrammingLanguage;
import com.sap.psr.vulas.shared.json.JacksonUtil;
import com.sap.psr.vulas.shared.util.FileUtil;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MainController.class,webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class BugControllerTest {
	
	private MediaType contentType = new MediaType(MediaType.APPLICATION_JSON.getType(),
            MediaType.APPLICATION_JSON.getSubtype(),
            Charset.forName("utf8"));

    private MockMvc mockMvc;
    private HttpMessageConverter<?> mappingJackson2HttpMessageConverter;

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private ConstructChangeRepository ccRepository;
    
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        this.mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream().filter(
                new Predicate<HttpMessageConverter<?>>() {
					@Override
					public boolean test(HttpMessageConverter<?> hmc) {
						return hmc instanceof MappingJackson2HttpMessageConverter;
					}
				}).findAny().get();

        Assert.assertNotNull("the JSON message converter must not be null",
                this.mappingJackson2HttpMessageConverter);
    }

    @Before
    public void setup() throws Exception {
        this.mockMvc = webAppContextSetup(webApplicationContext).build();
        this.bugRepository.deleteAll();
    }
    
    /**
     * Rest-read non-existing bug.
     * @throws Exception
     */
    @Test
    public void testGetNotFound() throws Exception {
        mockMvc.perform(get("/bugs/CVE-xxxx-yyyy"))
                .andExpect(status().isNotFound());
    }
    
    /**
     * Repo-save and rest-get.
     * @throws Exception
     */
    @Test
    public void testGetBug() throws Exception {
    	final Bug bug = this.createExampleBug();
    	this.bugRepository.customSave(bug,true);
    	mockMvc.perform(get("/bugs/"
                + bug.getBugId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    }
    
    /**
     * Rest-post and rest-get.
     * @throws Exception
     */
    @Test
    public void testPost() throws Exception {
    	final Bug bug = this.createExampleBug();
    	
    	// Rest-post
    	final MockHttpServletRequestBuilder post_builder = post("/bugs/")
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(post_builder)	
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	
    	// Rest-get
    	final MockHttpServletRequestBuilder get_builder = get("/bugs/" + bug.getBugId());
    	mockMvc.perform(get_builder)	
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    }
    
    /**
     * Rest-post and rest-get.
     * @throws Exception
     */
    @Test
    public void testPostCVE20140050() throws Exception {
    	final Bug bug = (Bug)JacksonUtil.asObject(FileUtil.readFile(Paths.get("./src/test/resources/real_examples/bug_CVE-2014-0050.json")), Bug.class);
    	
    	// Rest-post
    	final MockHttpServletRequestBuilder post_builder = post("/bugs/")
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(post_builder)	
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is("CVE-2014-0050")));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	
    	// Rest-get
    	final MockHttpServletRequestBuilder get_builder = get("/bugs/" + bug.getBugId());
    	mockMvc.perform(get_builder)	
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is("CVE-2014-0050")));
    }
    
    /**
     * Duplicate rest-post.
     * @throws Exception
     */
    @Test
    public void testDuplicatePost() throws Exception {
    	final Bug bug = this.createExampleBug();
    	
    	// Rest-post
    	final MockHttpServletRequestBuilder post_builder = post("/bugs/")
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(post_builder)	
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	
    	// Rest-post
    	mockMvc.perform(post_builder)	
                .andExpect(status().isConflict());
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    }
    
    /**
     * Rest-post and rest-delete.
     * @throws Exception
     */
    @Test
    public void testDelete() throws Exception {
    	final Bug bug = this.createExampleBug();
    	
    	// Rest-post
    	final MockHttpServletRequestBuilder post_builder = post("/bugs/")
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(post_builder)	
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	
    	// Rest-delete
    	final MockHttpServletRequestBuilder get_builder = delete("/bugs/" + bug.getBugId());
    	mockMvc.perform(get_builder)	
                .andExpect(status().isOk());
    	
    	// Repo must be empty
    	assertEquals(0, this.bugRepository.count());
    }
    
    /**
     * Rest-post and rest-put.
     * @throws Exception
     */
    @Test
    public void testPostPut() throws Exception {
    	final Bug bug = this.createExampleBug();
    	
    	// Rest-post
    	final MockHttpServletRequestBuilder post_builder = post("/bugs/")
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(post_builder)	
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	
    	// Rest-put
    	final MockHttpServletRequestBuilder put_builder = put("/bugs/" + bug.getBugId())
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(put_builder)	
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    }
    
    /**
     * Rest-post, change and rest-put.
     * @throws Exception
     */
    @Test
    public void testPostChangePut() throws Exception {
    	final Bug bug = this.createExampleBug();
    	
    	// Rest-post
    	final MockHttpServletRequestBuilder post_builder = post("/bugs/")
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(post_builder)	
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	assertEquals(2, this.ccRepository.count());
    	
    	bug.getConstructChanges().clear();
    	final ConstructId cid = new ConstructId(ProgrammingLanguage.JAVA, ConstructType.CLAS, "com.acme.Foo");
    	final ConstructChange cc3 = new ConstructChange("svn.apache.org", "123456", "/branch/1.x/src/main/java/com/acme/FooTestBar.java", cid, Calendar.getInstance(), ConstructChangeType.MOD);
    	bug.getConstructChanges().add(cc3);
    	
    	// Rest-put
    	final MockHttpServletRequestBuilder put_builder = put("/bugs/" + bug.getBugId())
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(put_builder)	
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.bugId", is(BUG_ID)))
                .andExpect(jsonPath("$.reference[0]", is(BUG_URL1)))
                .andExpect(jsonPath("$.reference[1]", is(BUG_URL2)))
                .andExpect(jsonPath("$.description", is(BUG_DESCR)));
    	
    	// Repo must contain 1
    	assertEquals(1, this.bugRepository.count());
    	assertEquals(1, this.ccRepository.count());
    }
    
    /**
     * Rest-put non-existing bug.
     * @throws Exception
     */
    @Test
    public void testPutNotFound() throws Exception {
    	final Bug bug = this.createExampleBug();
    	
    	// Rest-put
    	final MockHttpServletRequestBuilder put_builder = put("/bugs/" + bug.getBugId())
    			.content(JacksonUtil.asJsonString(bug).getBytes())
				.contentType(MediaType.APPLICATION_JSON)
				.accept(MediaType.APPLICATION_JSON);
    	mockMvc.perform(put_builder)	
                .andExpect(status().isNotFound());
    	
    	// Repo must contain 1
    	assertEquals(0, this.bugRepository.count());
    }
    	
    /*@Test
    public void postSingleBug() throws Exception {
    	//https://shdhumale.wordpress.com/2011/07/07/code-to-compress-and-decompress-json-object/
    }*/
    
    private static final String BUG_JSON = "{\"id\":1,\"bugId\":\"CVE-2014-0050\",\"source\":\"NVD\",\"description\":\"MultipartStream.java in Apache Commons FileUpload before 1.3.1, as used in Apache [...]\",\"url\":\"https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2014-0050\",\"constructChanges\":[{\"repo\":\"svn.apache.org\",\"commit\":\"123456\",\"path\":\"/branch/1.x/src/main/java/com/acme/Foo.java\",\"constructId\":{\"lang\":\"JAVA\",\"type\":\"CLAS\",\"qname\":\"com.acme.Foo\"},\"committedAt\":\"2016-05-13T14:35:50.274+0000\",\"changeType\":\"MOD\"},{\"repo\":\"svn.apache.org\",\"commit\":\"123456\",\"path\":\"/trunk/src/main/java/com/acme/Foo.java\",\"constructId\":{\"lang\":\"JAVA\",\"type\":\"CLAS\",\"qname\":\"com.acme.Foo\"},\"committedAt\":\"2016-05-13T14:35:50.274+0000\",\"changeType\":\"MOD\"}],\"countConstructChanges\":2}";
    private static final String BUG_ID = "CVE-2014-0050";
    private static final String BUG_URL1 = "https://web.nvd.nist.gov/view/vuln/detail?vulnId=CVE-2014-0050";
    private static final String BUG_URL2 = "http://svn.apache.org/r1565143";
    private static final String BUG_DESCR = "MultipartStream.java in Apache Commons FileUpload before 1.3.1, as used in Apache Tomcat, JBoss Web, and other products, allows remote attackers to cause a denial of service (infinite loop and CPU consumption) via a crafted Content-Type header that bypasses a loop&#039;s intended exit conditions.";
        
    /**
     * Creates a transient bug.
     * @return
     */
    private final Bug createExampleBug() {
    	Collection<String> references = new ArrayList<String>();
    	references.add(BUG_URL1);
    	references.add(BUG_URL2);
    	final ConstructId cid = new ConstructId(ProgrammingLanguage.JAVA, ConstructType.CLAS, "com.acme.Foo");
    	final ConstructChange cc1 = new ConstructChange("svn.apache.org", "123456", "/trunk/src/main/java/com/acme/Foo.java", cid, Calendar.getInstance(), ConstructChangeType.MOD);
    	final ConstructChange cc2 = new ConstructChange("svn.apache.org", "123456", "/branch/1.x/src/main/java/com/acme/Foo.java", cid, Calendar.getInstance(), ConstructChangeType.MOD);
    	final Bug b = new Bug(BUG_ID, BUG_DESCR, references);
    	b.setOrigin(BugOrigin.PUBLIC);
    	b.setMaturity(ContentMaturityLevel.READY);
    	cc1.setBug(b);
    	cc2.setBug(b);
    	Set<ConstructChange> ccs = new HashSet<ConstructChange>();
    	ccs.add(cc1); ccs.add(cc2);
    	b.setConstructChanges(ccs);
    	return b;
    }
}