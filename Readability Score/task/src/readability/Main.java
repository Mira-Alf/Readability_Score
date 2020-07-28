package readability;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        try {
            testStageFour(args[0]);
        } catch(Exception e) {
            System.out.println("error: "+e.getMessage());
        }
    }

    public static void testStageOne() {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();

        if(line.length() > 100)
            System.out.println("HARD");
        else
            System.out.println("EASY");
    }

    public static void testStageTwo() {
        Scanner scanner = new Scanner(System.in);
        String line = scanner.nextLine();
        TextProcessor processor = new TextProcessor(line);

        long numSentences = processor.getNumberOfSentences();
        long numWords = processor.getNumberOfWords();
        if( numWords/numSentences <= 10 )
            System.out.println("EASY");
        else
            System.out.println("HARD");
    }

    public static void testStageThree(String argument) throws IOException {
        ProgramTemplate template = new ProgramTemplate(argument);
        template.displayStatistics();
        TextProcessor processor = template.getProcessor();
        ReadabilityIndexCalculator calculator =
                ReadabilityFactory.getCalculators("ARI", processor).get(0);
        calculator.calculateScore();
        System.out.printf("The score is: %.2f%n", calculator.getScore());
        System.out.printf("This text should be understood by %s year olds.%n",
                calculator.getAgeRange());
    }

    public static void testStageFour(String argument) throws IOException {
        ProgramTemplate template = new ProgramTemplate(argument);
        template.execute();
    }
}

class ReadabilityFactory {

    public static List<ReadabilityIndexCalculator> getCalculators(String index,
                                                                  TextProcessor processor) {
        index = index.toUpperCase();
        ReadabilityAlgorithm algo = ReadabilityAlgorithm.valueOf(index);
        List<ReadabilityIndexCalculator> calculators = new ArrayList<>();
        ReadabilityIndexCalculator ari = new ARIndexCalculator(processor),
                fk = new FKndexCalculator(processor), smog = new SmogIndexCalculator(processor),
                cl = new CLIndexCalculator(processor);
        switch(algo) {
            case ALL:
                calculators.add(ari);
                calculators.add(fk);
                calculators.add(smog);
                calculators.add(cl);
                break;
            case ARI:
                calculators.add(ari);
                break;
            case FK:
                calculators.add(fk);
                break;
            case SMOG:
                calculators.add(smog);
                break;
            case CL:
                calculators.add(cl);
                break;

        }
        return calculators;
    }
}

class ProgramTemplate {
    private TextProcessor processor;
    private StringBuilder inputLine;

    public ProgramTemplate(String argument) throws IOException {
        populateInputLine(argument);
    }

    public TextProcessor getProcessor() {
        return processor;
    }

    private void populateInputLine(String argument) throws IOException {
        inputLine = new StringBuilder();
        List<Character> characterList = FileUtil.readAllBytes(argument);
        characterList.forEach(c->inputLine.append(c));
    }

    public void execute() {
        displayStatistics();
        displayCalculatorScoreAndGrade(getCalculatorType());
    }

    private List<ReadabilityIndexCalculator> getCalculatorType() {
        System.out.println("Enter the score you want to calculate (ARI, FK, SMOG, CL, all): ");
        Scanner scanner = new Scanner(System.in);
        return ReadabilityFactory.getCalculators(scanner.nextLine(), processor);

    }

    public void displayStatistics() {
        processor = new TextProcessor(inputLine.toString());
        processor.displayStatistics();
    }

    private void displayCalculatorScoreAndGrade(List<ReadabilityIndexCalculator> calculators) {
        List<Integer> ageBounds = new ArrayList<>();
        calculators.forEach(calc -> {
            calc.calculateScore();
            ageBounds.add(calc.getAgeUpperBound());
            System.out.printf("%s: %.2f (about %d year olds).%n", calc.getTitle(),
                    calc.getScore(), calc.getAgeUpperBound());
        });
        double average = (double)ageBounds.stream()
                .mapToInt(a->a).sum()/ageBounds.size();
        System.out.printf("This text should be understood in average by %.2f year olds.%n", average );
    }
}

class FileUtil {
    public static List<Character> readAllBytes(String fileName) throws IOException {
        List<Character> characterList = new ArrayList<>();
        try(FileInputStream in = new FileInputStream(fileName)) {
            int c = in.read();
            while(c != -1) {
                characterList.add((char)c);
                c = in.read();
            }
        }
        return characterList;
    }
}

class TextProcessor {
    public static final String SENTENCE_SEP = "[\\.!?]";
    public static final String WORD_SEP = "\\s+";
    public static final String CHAR_MATCH = "\\S";
    public static final String VOWELS = "aeiouy";
    public static final List<Character> VOWELS_LIST = new ArrayList<>();

    private String inputLine;

    public TextProcessor(String inputLine) {
        this.inputLine = inputLine;
        populateVowels();
    }

    private void populateVowels() {
        VOWELS.chars().forEach(v->VOWELS_LIST.add((char)v));
        VOWELS.toUpperCase().chars().forEach(v->VOWELS_LIST.add((char)v));
    }

    public long getNumberOfSentences() {
        return Arrays.stream(inputLine.split(SENTENCE_SEP))
                .filter(s->s.trim().length()>0)
                .count();
    }

    public long getNumberOfWords() {
        return Arrays.stream(inputLine.split(WORD_SEP))
                .filter(w->w.length()>0)
                .count();
    }

    public long getNumberOfCharacters() {
        return inputLine.chars()
                .filter(c->{
                    char ch = (char)c;
                    return String.valueOf(ch).matches(CHAR_MATCH);
                }).count();
    }

    private int getSyllables(String word) {
        if(word.matches("\\w*[a|e|i|o|u][\\\\.|!|?]"))
            word = word.substring(0, word.length()-1);
        int sValue = 0;
        int numVowels = 0;
        for(int i = 0; i < word.length(); ) {
            char c = word.charAt(i);
            if(VOWELS_LIST.contains(c)) {
                if(i < word.length()-1 && VOWELS_LIST.contains(word.charAt(i+1))) {
                    i++;
                    continue;
                } else {
                    if(!(i==word.length()-1 && Character.toLowerCase(c) == 'e')) {
                        numVowels++;
                    }
                }
            }
            i++;
        }
        if(numVowels == 0)
            sValue++;
        else
            sValue += (long)numVowels;
        return sValue;
    }

    public long getNumberOfSyllables() {
        List<String> words = Arrays.stream(inputLine.toString().split(WORD_SEP))
                .filter(w->w.length()>0).collect(Collectors.toList());
        List<Long> numSyllables = new ArrayList<>();
        numSyllables.add(0L);
        words.forEach(word -> {
            long sValue = getSyllables(word);
            long syllablesValue = numSyllables.get(0);
            numSyllables.set(0, syllablesValue+sValue);
        });
        return numSyllables.get(0);
    }

    public long getNumberOfPolySyllables() {
        List<String> words = Arrays.stream(inputLine.toString().split(WORD_SEP))
                .filter(w->w.length()>0).collect(Collectors.toList());
        List<Long> polySyllables = new ArrayList<>();
        polySyllables.add(0L);
        words.forEach(word -> {
            int sValue = getSyllables(word);
            if(sValue > 2) {
                long syllablesValue = polySyllables.get(0);
                polySyllables.set(0, syllablesValue+1);
            }
        });
        return polySyllables.get(0);
    }

    public double getL() {
        return (double)getNumberOfCharacters()/getNumberOfWords()*100.0;
    }

    public double getS() {
        return (double)getNumberOfSentences()/getNumberOfWords()*100.0;
    }

    public void displayStatistics() {
        displayText();
        displayWords();
        displaySentences();
        displayCharacters();
        displaySyllables();
        displayPolySyllables();
    }

    protected void displayText() {
        System.out.println("The text is:\n"+inputLine);
    }

    protected void displaySentences() {
        System.out.println("Sentences: "+getNumberOfSentences());
    }

    protected void displayWords() {
        System.out.println("Words: "+getNumberOfWords());
    }

    protected void displayCharacters() {
        System.out.println("Characters: "+getNumberOfCharacters());
    }

    protected void displaySyllables() {
        System.out.println("Syllables: "+getNumberOfSyllables());
    }

    protected void displayPolySyllables() {
        System.out.println("Polysyllables: "+getNumberOfPolySyllables());
    }
}

abstract class ReadabilityIndexCalculator {
    protected double score;
    protected TextProcessor processor;
    protected ReadabilityAlgorithm algo;

    public ReadabilityIndexCalculator(TextProcessor processor) {
        this.processor = processor;
    }
    public abstract void calculateScore();
    public abstract String getTitle();

    public double getScore() {
        return this.score;
    }

    public String getAgeRange() {
        ReadingCategory category = ReadingCategory.getReadingCategory(score);
        return category.getAgeRange();
    }

    public int getAgeUpperBound() {
        ReadingCategory category = ReadingCategory.getReadingCategory(score);
        return category.getUpperBoundAge();
    }
}

class ARIndexCalculator extends ReadabilityIndexCalculator {

    public ARIndexCalculator(TextProcessor processor) {
        super(processor);
        algo = ReadabilityAlgorithm.ARI;
    }
    @Override
    public void calculateScore() {
        score = 4.71 * ((double)processor.getNumberOfCharacters()/processor.getNumberOfWords())
                + 0.5 * ((double)processor.getNumberOfWords()/processor.getNumberOfSentences()) - 21.43;
    }

    @Override
    public String getTitle() {
        return "Automated Readability Index";
    }
}

class FKndexCalculator extends ReadabilityIndexCalculator {

    public FKndexCalculator(TextProcessor processor) {
        super(processor);
        algo = ReadabilityAlgorithm.FK;
    }

    @Override
    public void calculateScore() {
        score = 0.39 * ((double)processor.getNumberOfWords()/processor.getNumberOfSentences()) +
                11.8 * ((double)processor.getNumberOfSyllables()/processor.getNumberOfWords()) - 15.59;
    }

    @Override
    public String getTitle() {
        return "Flesch–Kincaid readability tests";
    }
}

class SmogIndexCalculator extends ReadabilityIndexCalculator {

    public SmogIndexCalculator(TextProcessor processor) {
        super(processor);
        algo = ReadabilityAlgorithm.SMOG;
    }

    @Override
    public void calculateScore() {
        score = 1.043 * Math.sqrt(processor.getNumberOfPolySyllables()*30/processor.getNumberOfSentences())
                + 3.1291;
    }

    @Override
    public String getTitle() {
        return "Simple Measure of Gobbledygook";
    }
}

class CLIndexCalculator extends ReadabilityIndexCalculator {
    public CLIndexCalculator(TextProcessor processor) {
        super(processor);
        algo = ReadabilityAlgorithm.CL;
    }

    @Override
    public void calculateScore() {
        score = 0.0588 * processor.getL() - 0.296*processor.getS() - 15.8;
    }

    @Override
    public String getTitle() {
        return "Coleman–Liau index";
    }


}

enum ReadingCategory {
    KINDERGARTEN("5-6"), FIRST_OR_SECOND_GRADE("6-7"), THIRD_GRADE("7-9"), FOURTH_GRADE("9-10"),
    FIFTH_GRADE("10-11"), SIXTH_GRADE("11-12"), SEVENTH_GRADE("12-13"), EIGHTH_GRADE("13-14"),
    NINTH_GRADE("14-15"), TENTH_GRADE("15-16"), ELEVENTH_GRADE("16-17"), TWELFTH_GRADE("17-18"),
    COLLEGE_STUDENT("18-24"), PROFESSOR("24+");

    private String ageRange;

    ReadingCategory(String ageRange) {
        this.ageRange = ageRange;
    }

    public String getAgeRange() {
        return this.ageRange;
    }

    public static ReadingCategory getReadingCategory(double score) {
        int scoreValue = (int) Math.round(score);
        switch(scoreValue) {
            case 1:
                return KINDERGARTEN;
            case 2:
                return FIRST_OR_SECOND_GRADE;
            case 3:
                return THIRD_GRADE;
            case 4:
                return FOURTH_GRADE;
            case 5:
                return FIFTH_GRADE;
            case 6:
                return SIXTH_GRADE;
            case 7:
                return SEVENTH_GRADE;
            case 8:
                return EIGHTH_GRADE;
            case 9:
                return NINTH_GRADE;
            case 10:
                return TENTH_GRADE;
            case 11:
                return ELEVENTH_GRADE;
            case 12:
                return TWELFTH_GRADE;
            case 13:
                return COLLEGE_STUDENT;
            case 14:
                return PROFESSOR;
        }
        return null;
    }

    public int getUpperBoundAge() {
        String ageRange = this.getAgeRange();
        if(this.name().equals("PROFESSOR")) {
             ageRange = ageRange.substring(0, ageRange.length()-1);
             return Integer.valueOf(ageRange);
        }
        else {
            int index = ageRange.length()-2;
            return Integer.valueOf(ageRange.substring(index));
        }
    }

}

enum ReadabilityAlgorithm {
    ARI, FK, SMOG, CL, ALL;
}