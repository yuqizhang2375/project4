package org.lappsgrid.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.lappsgrid.api.ProcessingService;
import org.lappsgrid.discriminator.Discriminators.Uri;
import org.lappsgrid.metadata.IOSpecification;
import org.lappsgrid.metadata.ServiceMetadata;
import org.lappsgrid.serialization.Data;
import org.lappsgrid.serialization.DataContainer;
import org.lappsgrid.serialization.LappsIOException;
import org.lappsgrid.serialization.Serializer;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

/**
 * AnswerScoring: score each answer based on its overlap with the question
 * @author yuqizhang
 *
 */
public class AnswerScoring implements ProcessingService{
  private String metadata;
  private int n;

  public AnswerScoring() {
      metadata = generateMetadata();
      this.n = 1;
  }
  public AnswerScoring(int n){
      metadata = generateMetadata();
      this.n = n;
  }

  /**
   * Generate Metadata
   * Input format: TEXT/LAPPS
   * Output format: LAPPS
   * Annotation format: Uri.TOKEN
   * @return
   */
  private String generateMetadata() {
      // Create and populate the metadata object
      ServiceMetadata metadata = new ServiceMetadata();

      // Populate metadata using setX() methods
      metadata.setName(this.getClass().getName());
      metadata.setDescription("Whitespace tokenizer");
      metadata.setVersion("1.0.0-SNAPSHOT");
      metadata.setVendor("http://www.lappsgrid.org");
      metadata.setLicense(Uri.APACHE2);

      // JSON for input information
      IOSpecification requires = new IOSpecification();
      requires.addFormat(Uri.TEXT);           // Plain text (form)
      requires.addFormat(Uri.LAPPS);            // LIF (form)
      requires.addLanguage("en");             // Source language
      requires.setEncoding("UTF-8");

      // JSON for output information
      IOSpecification produces = new IOSpecification();
      produces.addFormat(Uri.LAPPS);          // LIF (form) synonymous to LIF
      produces.addAnnotation(Uri.TOKEN);      // Tokens (contents)
      requires.addLanguage("en");             // Target language
      produces.setEncoding("UTF-8");

      // Embed I/O metadata JSON objects
      metadata.setRequires(requires);
      metadata.setProduces(produces);

      // Serialize the metadata to a string and return
      Data<ServiceMetadata> data = new Data<ServiceMetadata>(Uri.META, metadata);
      return data.asPrettyJson();
  }

  @Override
  /**
   * getMetadata simply returns metadata populated in the constructor
   */
  public String getMetadata() {
      return metadata;
  }

  @Override
  /**
   * Give score for each answer based on the overlap between the answer and the question
   */
  public String execute(String input) {
      // Step #1: Parse the input.
      Data data = Serializer.parse(input, Data.class);

      // Step #2: Check the discriminator
      final String discriminator = data.getDiscriminator();
      if (discriminator.equals(Uri.ERROR)) {
          // Return the input unchanged.
          return input;
      }

      // Step #3: Extract the text.
      Container container = null;
      if (discriminator.equals(Uri.TEXT)) {
          container = new Container();
          container.setText(data.getPayload().toString());
      }
      else if (discriminator.equals(Uri.LAPPS)) {
          container = new Container((Map) data.getPayload());
      }
      else {
          // This is a format we don't accept.
          String message = String.format("Unsupported discriminator type: %s", discriminator);
          return new Data<String>(Uri.ERROR, message).asJson();
      }

      // Step #4: Create a new View
      List<View> views = container.getViews();
      int lastview = views.size()-1;
      View oldview = container.getView(lastview);
      List<Annotation> annotations = oldview.getAnnotations();
      
      
      View view = container.newView();

      Annotation question = annotations.get(0);
      List<Map<String, Integer>> quesngram = new ArrayList<>();
      int totalques = 0;
      try {
        //for(int i = 1; i<4; i++){
          String key = this.n +"-Gram";
          Map<String, Integer> quesMap = question.getFeatureMap(key);
          quesngram.add(quesMap);
          for(String word: quesMap.keySet()){
            int tempcount = quesMap.get(word);
            totalques+=tempcount;
          //}
          
        }
        
 
      } catch (LappsIOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      int id = -1;
      for(int i = 1; i<annotations.size();i++){
        Annotation ans = annotations.get(i);
        Annotation a = view.newAnnotation(ans.getId(), Uri.TOKEN, ans.getStart(),
                ans.getEnd());
        double score = 0;
        //for(int j = 1; j<4;j++){
          String key = this.n +"-Gram";
          try {
            Map<String, Integer> ansmap = ans.getFeatureMap(key);
            for(String quesword:quesngram.get(0).keySet()){
              if(ansmap.containsKey(quesword))
                score += Math.min(ansmap.get(quesword), quesngram.get(0).get(quesword));
            }
          } catch (LappsIOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        //}
        score = score/totalques;
        a.addFeature("Score", Double.toString(score));
        a.addFeature("Group", ans.getId().split("-")[0]);
        
      }
      
      
      

      // Step #6: Update the view's metadata. Each view contains metadata about the
      // annotations it contains, in particular the name of the tool that produced the
      // annotations.
      view.addContains(Uri.TOKEN, this.getClass().getName(), "answerscoring");

      // Step #7: Create a DataContainer with the result.
      data = new DataContainer(container);

      // Step #8: Serialize the data object and return the JSON.
      return data.asPrettyJson();
  }
}
