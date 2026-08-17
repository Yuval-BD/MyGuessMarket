package gm.engine.exception;

import java.util.List;

public class XmlContentException extends GuessMarketException {

    private final List<String> problems;

    public XmlContentException(List<String> problems) {
        super(buildMessage(problems));
        this.problems = List.copyOf(problems);
    }

    public List<String> getProblems() {
        return problems;
    }

    private static String buildMessage(List<String> problems) {
        StringBuilder sb = new StringBuilder();
        sb.append("The file could not be loaded. ")
                .append(problems.size())
                .append(problems.size() == 1 ? " problem was found:" : " problems were found:")
                .append(System.lineSeparator());
        for (int i = 0; i < problems.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(problems.get(i)).append(System.lineSeparator());
        }
        return sb.toString();
    }
}
