package checkout.mediator;

import checkout.model.Quote;

public class SummaryComponent extends Component {
    private Quote quote;

    public void show(Quote q) {
        this.quote = q;
    }

    public Quote quote() {
        return quote;
    }
}
