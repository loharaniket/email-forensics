package com.emailforensics.backend.content;

import com.emailforensics.backend.dto.content.MlEvaluationMetricsDto;
import org.springframework.stereotype.Service;
import org.tribuo.Feature;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TribuoBaselineService {

    public static final String MODEL_VERSION = "tribuo-logreg-tfidf-v1.0.0";
    private static final String LABEL_PHISHING = "PHISHING";
    private static final String LABEL_BENIGN = "BENIGN";

    public static class PredictionResult {
        private final String predictedLabel;
        private final Double probability;
        private final String mlStatus;

        public PredictionResult(String predictedLabel, Double probability, String mlStatus) {
            this.predictedLabel = predictedLabel;
            this.probability = probability;
            this.mlStatus = mlStatus;
        }

        public String getPredictedLabel() {
            return predictedLabel;
        }

        public Double getProbability() {
            return probability;
        }

        public String getMlStatus() {
            return mlStatus;
        }
    }

    private static class Sample {
        final String text;
        final String label;

        Sample(String text, String label) {
            this.text = text;
            this.label = label;
        }
    }

    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are",
            "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but",
            "by", "could", "did", "do", "does", "doing", "down", "during", "each", "few", "for", "from",
            "further", "had", "has", "have", "having", "he", "her", "here", "hers", "herself", "him",
            "himself", "his", "how", "i", "if", "in", "into", "is", "it", "its", "itself", "just",
            "me", "more", "most", "my", "myself", "no", "nor", "not", "now", "of", "off", "on", "once",
            "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same",
            "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them",
            "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too",
            "under", "until", "up", "very", "was", "we", "were", "what", "when", "where", "which",
            "while", "who", "whom", "why", "with", "would", "you", "your", "yours", "yourself", "yourselves"
    ));

    private static final Pattern WORD_SPLIT = Pattern.compile("[^a-zA-Z0-9]+");

    private final LabelFactory labelFactory = new LabelFactory();
    private final Map<String, Double> idfMap = new HashMap<>();
    private final Set<String> vocabulary = new HashSet<>();
    private Model<Label> model;
    private MlEvaluationMetricsDto modelMetrics;

    public TribuoBaselineService() {
        trainAndEvaluate();
    }

    private void trainAndEvaluate() {
        List<Sample> trainingData = getTrainingCorpus();
        List<Sample> holdoutTestData = getHoldoutTestCorpus();

        fitIdfOnTrainingData(trainingData);

        MutableDataset<Label> trainDataset = new MutableDataset<>(
                new SimpleDataSourceProvenance("Synthetic Forensic Training Corpus", labelFactory),
                labelFactory
        );

        for (Sample s : trainingData) {
            ArrayExample<Label> example = createExample(s.text, s.label);
            trainDataset.add(example);
        }

        LogisticRegressionTrainer trainer = new LogisticRegressionTrainer();
        this.model = trainer.train(trainDataset);

        MutableDataset<Label> testDataset = new MutableDataset<>(
                new SimpleDataSourceProvenance("Synthetic Forensic Holdout Test Corpus", labelFactory),
                labelFactory
        );

        for (Sample s : holdoutTestData) {
            ArrayExample<Label> example = createExample(s.text, s.label);
            testDataset.add(example);
        }

        LabelEvaluation eval = new LabelEvaluator().evaluate(this.model, testDataset);

        Label phishLabel = new Label(LABEL_PHISHING);
        double precision = eval.precision(phishLabel);
        double recall = eval.recall(phishLabel);
        double f1 = eval.f1(phishLabel);
        double accuracy = eval.accuracy();

        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;

        for (Sample s : holdoutTestData) {
            PredictionResult res = predict(s.text);
            if (LABEL_PHISHING.equals(s.label)) {
                if (LABEL_PHISHING.equals(res.getPredictedLabel())) {
                    tp++;
                } else {
                    fn++;
                }
            } else {
                if (LABEL_PHISHING.equals(res.getPredictedLabel())) {
                    fp++;
                } else {
                    tn++;
                }
            }
        }

        this.modelMetrics = new MlEvaluationMetricsDto(
                Double.isNaN(precision) ? 1.0 : precision,
                Double.isNaN(recall) ? 1.0 : recall,
                Double.isNaN(f1) ? 1.0 : f1,
                accuracy,
                tp,
                fp,
                tn,
                fn,
                trainingData.size(),
                holdoutTestData.size()
        );
    }

    private void fitIdfOnTrainingData(List<Sample> samples) {
        Map<String, Integer> docFrequencies = new HashMap<>();
        int docCount = samples.size();

        for (Sample s : samples) {
            List<String> tokens = tokenize(s.text);
            Set<String> uniqueInDoc = new HashSet<>(tokens);
            for (String t : uniqueInDoc) {
                docFrequencies.put(t, docFrequencies.getOrDefault(t, 0) + 1);
            }
        }

        for (Map.Entry<String, Integer> entry : docFrequencies.entrySet()) {
            if (entry.getValue() >= 1) {
                String term = entry.getKey();
                double idf = Math.log((double) (docCount + 1) / (entry.getValue() + 1)) + 1.0;
                this.idfMap.put(term, idf);
                this.vocabulary.add(term);
            }
        }
    }

    private ArrayExample<Label> createExample(String text, String labelName) {
        Label label = labelName != null ? new Label(labelName) : new Label(LABEL_BENIGN);
        List<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return null;
        }

        Map<String, Integer> termCounts = new HashMap<>();
        for (String t : tokens) {
            if (vocabulary.contains(t)) {
                termCounts.put(t, termCounts.getOrDefault(t, 0) + 1);
            }
        }

        if (termCounts.isEmpty()) {
            return null;
        }

        List<Feature> features = new ArrayList<>();
        double totalTokens = tokens.size();
        for (Map.Entry<String, Integer> entry : termCounts.entrySet()) {
            String term = entry.getKey();
            double tf = entry.getValue() / totalTokens;
            double idf = idfMap.getOrDefault(term, 1.0);
            double tfidf = tf * idf;
            features.add(new Feature(term, tfidf));
        }

        return new ArrayExample<>(label, features);
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        String[] raw = WORD_SPLIT.split(text.toLowerCase(Locale.ROOT));
        List<String> tokens = new ArrayList<>();
        for (String r : raw) {
            if (r.length() >= 3 && !STOPWORDS.contains(r)) {
                tokens.add(r);
            }
        }
        return tokens;
    }

    public PredictionResult predict(String text) {
        if (text == null || text.isBlank()) {
            return new PredictionResult(LABEL_BENIGN, 0.0, "SKIPPED_EMPTY_TEXT");
        }

        ArrayExample<Label> example = createExample(text, null);
        if (example == null) {
            return new PredictionResult("INSUFFICIENT_DATA", 0.0, "SKIPPED_UNSEEN_VOCABULARY");
        }

        try {
            Prediction<Label> prediction = model.predict(example);
            String predictedLabel = prediction.getOutput().getLabel();
            Map<String, Label> dist = prediction.getOutputScores();
            Label phishScore = dist.get(LABEL_PHISHING);
            double phishProb = phishScore != null ? phishScore.getScore() : 0.0;
            return new PredictionResult(predictedLabel, phishProb, "EVALUATED");
        } catch (IllegalArgumentException e) {
            return new PredictionResult("INSUFFICIENT_DATA", 0.0, "SKIPPED_UNSEEN_VOCABULARY");
        }
    }

    public MlEvaluationMetricsDto getModelMetrics() {
        return modelMetrics;
    }

    public String getModelVersion() {
        return MODEL_VERSION;
    }

    private List<Sample> getTrainingCorpus() {
        return List.of(
                new Sample("Urgent notice: your corporate password will expire in 2 hours. Click here to verify your account credentials immediately or access will be suspended.", LABEL_PHISHING),
                new Sample("Action required: Multi-Factor Authentication device reset initiated. Scan the security QR code to confirm identity and restore session access.", LABEL_PHISHING),
                new Sample("Immediate payment required for overdue invoice INV-99021. Please remit wire transfer to the updated routing and account numbers attached.", LABEL_PHISHING),
                new Sample("Human Resources notice: please confirm your direct deposit details and enter your employee portal password to avoid payroll processing disruption.", LABEL_PHISHING),
                new Sample("I am the CEO and currently in a confidential client meeting. Wire $45,000 immediately to the attached vendor. Do not call or discuss this with anyone.", LABEL_PHISHING),
                new Sample("Security alert: unauthorized login detected from suspicious IP address. Log in immediately to verify your account and prevent termination.", LABEL_PHISHING),
                new Sample("You have won an uncollected financial grant. Provide your bank account details and wire transfer processing fee within 24 hours.", LABEL_PHISHING),
                new Sample("IT Helpdesk Alert: Server migration requires all staff to authenticate and re-enter passcodes via the secure web link below.", LABEL_PHISHING),
                new Sample("Your cloud storage subscription is overdue. Account locked. Update billing details and credit card information to retain files.", LABEL_PHISHING),
                new Sample("Critical tax rebate notification from IRS compliance: verify your social security number and banking routing numbers to claim refund.", LABEL_PHISHING),

                new Sample("Engineering team sync notes: review sprint backlog items and pull requests for the upcoming product release milestone next Wednesday.", LABEL_BENIGN),
                new Sample("Company holiday schedule: our offices will be closed on Friday in observance of the upcoming national holiday. Enjoy the long weekend.", LABEL_BENIGN),
                new Sample("Quarterly all-hands meeting recording and slides are now available on the intranet repository for all staff to review.", LABEL_BENIGN),
                new Sample("Reminder: Code review guidelines updated. Please check the documentation in Git for formatting rules and testing standards.", LABEL_BENIGN),
                new Sample("Facilities update: routine elevator maintenance is scheduled for Tuesday morning from 9am to 11am on the west wing.", LABEL_BENIGN),
                new Sample("Monthly customer newsletter: discover the new dashboard analytics features, upcoming webinars, and release notes for version 2.4.", LABEL_BENIGN),
                new Sample("Lunch and learn session: join us in the main cafeteria or on Zoom for a presentation on modern cloud architecture practices.", LABEL_BENIGN),
                new Sample("Welcome new team members joining our engineering and design divisions this week! Please say hello in the team chat channel.", LABEL_BENIGN),
                new Sample("Expense reports for September travel must be submitted through the finance software by Friday for timely reimbursement.", LABEL_BENIGN),
                new Sample("Annual software developer survey results have been compiled. Thank you to everyone who provided helpful feedback on our toolchain.", LABEL_BENIGN)
        );
    }

    private List<Sample> getHoldoutTestCorpus() {
        return List.of(
                new Sample("Urgent security alert: please verify your employee credentials and reset your password immediately to avoid suspension.", LABEL_PHISHING),
                new Sample("Final payment reminder: overdue invoice needs immediate wire transfer. Remit payment to routing details enclosed.", LABEL_PHISHING),
                new Sample("From the desk of the Director: confidential request. Send wire transfer to client immediately and keep this between us.", LABEL_PHISHING),
                new Sample("MFA verification needed: scan QR code to re-authenticate your mobile session and update security passcode.", LABEL_PHISHING),

                new Sample("Weekly project status update: milestone 3 deliverables completed on schedule with zero blockers reported by QA.", LABEL_BENIGN),
                new Sample("The office cafeteria menu for the coming week has been published on the internal wiki portal.", LABEL_BENIGN),
                new Sample("Team reminder: please submit your weekly timesheets before end of day Friday for payroll verification.", LABEL_BENIGN),
                new Sample("Architecture documentation and API specifications have been updated in the repository README files.", LABEL_BENIGN)
        );
    }
}
