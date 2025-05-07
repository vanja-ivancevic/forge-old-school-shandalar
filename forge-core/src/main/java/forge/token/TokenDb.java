package forge.token;

import com.google.common.collect.Maps;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.item.PaperToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class TokenDb implements ITokenDatabase {
    // Expected naming convention of scripts
    // token_name
    // minor_demon
    // marit_lage
    // gold

    // colors_power_toughness_cardtypes_sub_types_keywords
    // Some examples:
    // c_3_3_a_phyrexian_wurm_lifelink
    // w_2_2_knight_first_strike

    // The image names should be the same as the script name + _set
    // If that isn't found, consider falling back to the original token

    private final Map<String, PaperToken> tokensByName = Maps.newTreeMap(String.CASE_INSENSITIVE_ORDER);

    private final CardEdition.Collection editions;
    private final Map<String, CardRules> rulesByName;

    public TokenDb(Map<String, CardRules> rules, CardEdition.Collection editions) {
        this.rulesByName = rules;
        this.editions = editions;
    }

    public boolean containsRule(String rule) {
        return this.rulesByName.containsKey(rule);

    }
    @Override
    public PaperToken getToken(String tokenName) {
        return getToken(tokenName, CardEdition.UNKNOWN.getName());
    }

    public void preloadTokens() {
        for (CardEdition edition : this.editions) {
            for (String name : edition.getTokens().keySet()) {
                try {
                    getToken(name, edition.getCode());
                } catch(Exception e) {
                    System.out.println(name + "_" + edition.getCode() + " defined in Edition file, but not defined as a token script.");
                }
            }
        }
    }

    private PaperToken createAndCacheToken(String tokenName, CardEdition edition) {
        String fullName = String.format("%s_%s", tokenName, edition.getCode().toLowerCase());
        if (tokensByName.containsKey(fullName)) {
            return tokensByName.get(fullName);
        }
        CardRules rules = rulesByName.get(tokenName);
        if (rules == null) { //Should have been caught by the initial check in getToken
            throw new RuntimeException("Token script not found for " + tokenName + " when trying to create with edition " + edition.getCode());
        }
        PaperToken pt = new PaperToken(rules, edition, tokenName);
        tokensByName.put(fullName, pt);
        return pt;
    }

    @Override
    public PaperToken getToken(String tokenName, String editionHint) {
        if (!rulesByName.containsKey(tokenName)) {
            throw new RuntimeException("Token script not found: " + tokenName);
        }

        List<CardEdition> candidateEditions = new ArrayList<>();
        for (CardEdition ed : this.editions) {
            if (ed.getTokens().containsKey(tokenName)) {
                candidateEditions.add(ed);
            }
        }

        if (!candidateEditions.isEmpty()) {
            // Sort by date, oldest first.
            candidateEditions.sort(Comparator.comparing(CardEdition::getDate, Comparator.nullsLast(Comparator.naturalOrder())));
            return createAndCacheToken(tokenName, candidateEditions.get(0));
        }

        // Fallback to the original hint if the token wasn't found in any set
        // This case should ideally not be reached if token scripts are well-maintained alongside edition data.
        if (editionHint != null) {
            CardEdition hintEd = editions.get(editionHint.toUpperCase()); // Normalize hint for lookup
            if (hintEd != null && hintEd.getTokens().containsKey(tokenName)) {
                return createAndCacheToken(tokenName, hintEd);
            }
        }
        
        // If still not found, it means the token script is in rulesByName, but no edition provides it.
        throw new RuntimeException("Token " + tokenName + " could not be resolved to a specific printing. Hinted edition: " + editionHint);
    }

    @Override
    public PaperToken getToken(String tokenName, String edition, int artIndex) {
        return null;
    }

    @Override
    public PaperToken getTokenFromEditions(String tokenName, CardDb.CardArtPreference fromSet) {
        return null;
    }

    @Override
    public PaperToken getTokenFromEditions(String tokenName, Date printedBefore, CardDb.CardArtPreference fromSet) {
        return null;
    }

    @Override
    public PaperToken getTokenFromEditions(String tokenName, Date printedBefore, CardDb.CardArtPreference fromSet, int artIndex) {
        return null;
    }

    @Override
    public PaperToken getFoiled(PaperToken cpi) {
        return null;
    }

    @Override
    public int getPrintCount(String cardName, String edition) {
        return 0;
    }

    @Override
    public int getMaxPrintCount(String cardName) {
        return 0;
    }

    @Override
    public int getArtCount(String cardName, String edition) {
        return 0;
    }

    @Override
    public Collection<PaperToken> getUniqueTokens() {
        return null;
    }

    @Override
    public List<PaperToken> getAllTokens() {
        return new ArrayList<>(tokensByName.values());
    }

    @Override
    public List<PaperToken> getAllTokens(String tokenName) {
        return null;
    }

    @Override
    public List<PaperToken> getAllTokens(Predicate<PaperToken> predicate) {
        return null;
    }

    @Override
    public Predicate<? super PaperToken> wasPrintedInSets(List<String> allowedSetCodes) {
        return null;
    }

    @Override
    public Iterator<PaperToken> iterator() {
        return tokensByName.values().iterator();
    }
}
