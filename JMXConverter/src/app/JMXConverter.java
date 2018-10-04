package app;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class JMXConverter {
  public static void main(String[] args) throws ParserConfigurationException, IOException, SAXException, TransformerException {
    String filepath = args[0];
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    Document doc = docBuilder.parse(filepath);

    //ToDo check if Plan has several Thread groups
    //ToDo check if next Sibling of Thread tag isn't hashTree
    Element thread = (Element) doc.getElementsByTagName("ThreadGroup").item(0);
    Node hashTreeAfterThread = thread.getNextSibling();
    Element pythonAssertion = createPythonSampler(doc);
    hashTreeAfterThread.getNextSibling().appendChild(pythonAssertion);


    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
    DOMSource source = new DOMSource(doc);
    StreamResult result = new StreamResult(new File(filepath));
    transformer.transform(source, result);

    System.out.println("Done");


  }


  //ToDo join 2 methods in 1 and call with different parameters
  private static Element createPythonAssertion(Document doc) {
    Element sampler = doc.createElement("JSR223Assertion");
    sampler.setAttribute("guiclass", "TestBeanGUI");
    sampler.setAttribute("testclass", "JSR223Assertion");
    sampler.setAttribute("testname", "Python main assertion");
    sampler.setAttribute("enabled", "true");

    Element cacheKey = doc.createElement("stringProp");
    cacheKey.setAttribute("name", "cacheKey");
    cacheKey.setTextContent("true");
    sampler.appendChild(cacheKey);

    Element filename = doc.createElement("stringProp");
    filename.setAttribute("name", "filename");
    sampler.appendChild(filename);

    Element parameters = doc.createElement("stringProp");
    parameters.setAttribute("name", "parameters");
    sampler.appendChild(parameters);

    Element script = doc.createElement("stringProp");
    script.setAttribute("name", "script");
    script.setTextContent("from __future__ import unicode_literals\n" +
            "import re\n" +
            "\n" +
            "# # # # #  @@@@STANDART VARIABLES\n" +
            "\n" +
            "SubResults_array = SampleResult.getSubResults()\n" +
            "SubResults_count = len(SubResults_array)\n" +
            "http_response_code = SampleResult.getResponseCode()\n" +
            "vars.putObject(&apos;RAW_HTTP_TRACE&apos;, &apos;&apos;)\n" +
            "\n" +
            "# # # # # @@@@STANDART ASSERTION@@@@@\n" +
            "\n" +
            "def return_non_binary_response_body(result_instance, data_size):\n" +
            "    if result_instance.getDataType() == &apos;bin&apos;:\n" +
            "        return &apos;skipped binary data&apos;\n" +
            "    else:\n" +
            "        if data_size == &apos;all&apos;:\n" +
            "            ## logged in non binary data\n" +
            "            return result_instance.getResponseDataAsString()      \n" +
            "        elif data_size == &apos;reduced&apos;:\n" +
            "            ## logged in non binary data and reduce size to 100kB\n" +
            "            return result_instance.getResponseDataAsString()[:102400]\n" +
            "\n" +
            "if SubResults_count &gt; 0:\n" +
            "    ## --==get subsamples data in case of redirects or download extra turned ON==--\n" +
            "    ## limit to 5 subresults in case testing with extra resources\n" +
            "    http_response_data = &apos;&apos;\n" +
            "    for SubResult in SubResults_array:\n" +
            "        http_response_data += SubResult.getResponseHeaders() + return_non_binary_response_body(SubResult, &apos;all&apos;)\n" +
            "else:\n" +
            "    http_response_data = SampleResult.getResponseHeaders() + return_non_binary_response_body(SampleResult, &apos;all&apos;)\n" +
            "\n" +
            "    \n" +
            "## --==http response code check==--\n" +
            "if http_response_code[0] in (&apos;4&apos;, &apos;5&apos;):\n" +
            "    AssertionResult.setFailure(True)\n" +
            "    AssertionResult.setFailureMessage(&apos;HTTP response code &apos; + http_response_code)\n" +
            "## --==non http response code check==--\n" +
            "elif http_response_code[0] not in (&apos;1&apos;, &apos;2&apos;, &apos;3&apos;):\n" +
            "    AssertionResult.setFailure(True)\n" +
            "    AssertionResult.setFailureMessage(SampleResult.getResponseMessage().replace(&apos;Non HTTP response message: &apos;, &apos;&apos;))\n" +
            "\n" +
            "elif http_response_code == &apos;304&apos;:\n" +
            "    pass\n" +
            "\n" +
            "# # # # # @@@@END OF STANDART ASSERTION@@@@@\n" +
            "\n" +
            "# # # # # @@@@CUSTOM ASSERTION@@@@@@START\n" +
            "\n" +
            "## --==extractions check==--\n" +
            "## --==verifications check==-- \n" +
            "\n" +
            "## --==put full http trace into log==--\n" +
            "def compose_full_http_trace_message():\n" +
            "    full_http_trace = &apos;&apos;\n" +
            "    if SubResults_count &gt; 0:\n" +
            "        for SubResult in SubResults_array:\n" +
            "            full_http_trace += &apos;@@@request_data_start&gt;&gt;&gt;&apos; +  SubResult.getSamplerData()[:1024**2] + &apos;@@@request_headers_start&gt;&gt;&gt;&apos; + SubResult.getRequestHeaders() + &apos;@@@request_headers_end&apos;\n" +
            "            full_http_trace += &apos;@@@response_headers_start&gt;&gt;&gt;&apos; + SubResult.getResponseHeaders() + &apos;@@@response_body_start&gt;&gt;&gt;&apos; + return_non_binary_response_body(SubResult, &apos;reduced&apos;) + &apos;@@@response_body_end&apos;\n" +
            "    else:\n" +
            "        full_http_trace += &apos;@@@request_data_start&gt;&gt;&gt;&apos; + SampleResult.getSamplerData()[:1024**2] + &apos;@@@request_headers_start&gt;&gt;&gt;&apos; + SampleResult.getRequestHeaders() + &apos;@@@request_headers_end&apos;\n" +
            "        full_http_trace += &apos;@@@response_headers_start&gt;&gt;&gt;&apos; + SampleResult.getResponseHeaders() + &apos;@@@response_body_start&gt;&gt;&gt;&apos; + return_non_binary_response_body(SampleResult, &apos;reduced&apos;) + &apos;@@@response_body_end&apos;\n" +
            "\n" +
            "    AssertionResult.setFailureMessage(unicode(AssertionResult.getFailureMessage()).replace(&apos;~&apos;, &apos;@tilda&apos;))\n" +
            "    ## --==write to log http trace==--\n" +
            "    vars.putObject(&apos;RAW_HTTP_TRACE&apos;, &apos;@@@raw_trace_start&apos; + repr(full_http_trace) + &apos;@@@raw_trace_end&apos;)\n" +
            "thread_number = vars.getObject(&apos;random_thread_number&apos;)\n" +
            "threads_to_track_all_flow = 1\n" +
            "threads_to_track_errors = 200\n" +
            "\n" +
            "if thread_number &lt;= threads_to_track_all_flow:\n" +
            "    compose_full_http_trace_message()\n" +
            "elif thread_number &lt;= threads_to_track_all_flow + threads_to_track_errors:\n" +
            "    if AssertionResult.isFailure():\n" +
            "        compose_full_http_trace_message()");
    sampler.appendChild(script);

    Element scriptLanguage = doc.createElement("stringProp");
    scriptLanguage.setAttribute("name", "scriptLanguage");
    scriptLanguage.setTextContent("jython");
    sampler.appendChild(scriptLanguage);
    return sampler;
  }

  private static Element createPythonSampler(Document doc) {
    Element sampler = doc.createElement("JSR223Sampler");
    sampler.setAttribute("guiclass","TestBeanGUI");
    sampler.setAttribute("testclass", "JSR223Sampler");
    sampler.setAttribute("testname","set loop uuid");
    sampler.setAttribute("enabled","true");

    Element cacheKey = doc.createElement("stringProp");
    cacheKey.setAttribute("name","cacheKey");
    cacheKey.setTextContent("true");
    sampler.appendChild(cacheKey);

    Element filename = doc.createElement("stringProp");
    filename.setAttribute("name", "filename");
    sampler.appendChild(filename);

    Element parameters = doc.createElement("stringProp");
    parameters.setAttribute("name", "parameters");
    sampler.appendChild(parameters);

    Element script = doc.createElement("stringProp");
    script.setAttribute("name", "script");
    script.setTextContent("import uuid\n" +
            "### create THREAD_LOOP_UUID (unique per loop)\n" +
            "vars.putObject(&apos;THREAD_LOOP_UUID&apos;, str(uuid.uuid1()))\n" +
            "\n" +
            "import random\n" +
            "random_thread_number = random.randrange(1, ctx.getThreadGroup().getNumThreads() + 1)\n" +
            "vars.putObject(&apos;random_thread_number&apos;, random_thread_number)\n" +
            "\n" +
            "### create THREAD_ID (unique per thread)\n" +
            "THREAD_ID = vars.getObject(&apos;THREAD_ID&apos;)\n" +
            "if not THREAD_ID:\n" +
            "    vars.putObject(&apos;THREAD_ID&apos;, str(uuid.uuid1()))");
    sampler.appendChild(script);

    Element scriptLanguage = doc.createElement("stringProp");
    scriptLanguage.setAttribute("name", "scriptLanguage");
    scriptLanguage.setTextContent("jython");
    sampler.appendChild(scriptLanguage);
    return sampler;
  }


}
