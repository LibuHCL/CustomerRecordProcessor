package nl.rabobank.api.valueobject;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class XMLRecord {

  private final Integer reference;
  private final String description;
  private final BigDecimal startBalance;
  private final BigDecimal mutation;
  private final BigDecimal endBalance;
  private String accountNumber;

  private XMLRecord(RecordBuilder recordBuilder) {
    this.reference = recordBuilder.reference;
    this.accountNumber = recordBuilder.accountNumber;
    this.description = recordBuilder.description;
    this.startBalance = recordBuilder.startBalance;
    this.mutation = recordBuilder.mutation;
    this.endBalance = recordBuilder.endBalance;
  }

  public Integer validateReferenceAttribute() {
    return reference;
  }

  public static class RecordBuilder {
    private final Integer reference;
    private final String description;
    private final BigDecimal startBalance;
    private final BigDecimal mutation;
    private final BigDecimal endBalance;
    private String accountNumber;

    public RecordBuilder(
        Integer reference,
        String description,
        BigDecimal startBalance,
        BigDecimal mutation,
        BigDecimal endBalance) {
      this.reference = reference;
      this.description = description;
      this.startBalance = startBalance;
      this.mutation = mutation;
      this.endBalance = endBalance;
    }

    public RecordBuilder withAccountNumber(String accountNumber) {
      this.accountNumber = accountNumber;
      return this;
    }

    public XMLRecord build() {
      XMLRecord XMLRecord = new XMLRecord(this);
      return XMLRecord;
    }
  }
}
