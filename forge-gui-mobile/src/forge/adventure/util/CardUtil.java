package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.adventure.data.ConfigData;
import forge.adventure.data.GeneratedDeckData;
import forge.adventure.data.GeneratedDeckTemplateData;
import forge.adventure.data.RewardData;
import forge.card.*;
import forge.card.mana.ManaCostShard;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.CardPool;
import forge.deck.DeckgenUtil;
import forge.deck.io.DeckSerializer;
import forge.game.GameFormat;
import forge.item.BoosterPack;
import forge.item.PaperCard;
import forge.item.SealedTemplate;
import forge.item.generation.UnOpenedProduct;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.IterableUtil;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static forge.adventure.data.RewardData.generateAllCards;

/**
 * Utility class to deck generation and card filtering
 */
public class CardUtil {
    public static final class CardPredicate implements Predicate<PaperCard> {
        enum ColorType
        {
            Any,
            Colorless,
            MultiColor,
            MonoColor
        }
        private final List<CardRarity> rarities=new ArrayList<>();
        private final List<String> editions=new ArrayList<>();
        private final List<String> subType=new ArrayList<>();
        private final List<String> keyWords=new ArrayList<>();
        private final List<CardType.CoreType> type=new ArrayList<>();
        private final List<CardType.Supertype> superType=new ArrayList<>();
        private final List<Integer> manaCosts =new ArrayList<>();
        private final Pattern text;
        private final boolean matchAllSubTypes;
        private final boolean matchAllColors;
        private  int colors;
        private final ColorType colorType;
        private final boolean shouldBeEqual;
        private final List<String> deckNeeds=new ArrayList<>();
        private final String minDate;
        private final static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

        private static Date parseDate(String date) {
            if( date.length() <= 7 )
                date = date + "-01";
            try {
                return formatter.parse(date);
            } catch (Exception e) {
                return new Date();
            }
        }

        @Override
        public boolean test(final PaperCard card) {
            if(!this.rarities.isEmpty()&&!this.rarities.contains(card.getRarity()))
                return !this.shouldBeEqual;
            if(!this.editions.isEmpty()&&!this.editions.contains(card.getEdition())) {
                boolean found = false;
                List<PaperCard> allPrintings = FModel.getMagicDb().getCommonCards().getAllCards(card.getCardName());
                for (PaperCard c : allPrintings){
                    if (this.editions.contains(c.getEdition())) {
                        found = true;
                        break;
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if(!this.minDate.isEmpty()) {
                boolean found = false;
                List<PaperCard> allPrintings = FModel.getMagicDb().getCommonCards().getAllCards(card.getCardName());
                List<CardEdition> cardEditionList = new ArrayList<>();

                Date d = parseDate(this.minDate);

                for (CardEdition e : FModel.getMagicDb().getEditions()) {
                    if (e.getDate().before(d))
                        continue;
                    cardEditionList.add(e);
                }

                for (PaperCard c : allPrintings){
                    for (CardEdition e : cardEditionList) {
                        if (e.getCode().equals(c.getEdition())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found)
                    return !this.shouldBeEqual;
            }
            if(!this.manaCosts.isEmpty()&&!this.manaCosts.contains(card.getRules().getManaCost().getCMC()))
                return !this.shouldBeEqual;
            if(this.text!=null&& !this.text.matcher(card.getRules().getOracleText()).find())
                return !this.shouldBeEqual;

            if(this.matchAllColors)
            {
                if(!card.getRules().getColor().hasAllColors(this.colors))
                {
                    return !this.shouldBeEqual;
                }
            }

            if(this.colors!= MagicColor.ALL_COLORS)
            {
                if(!card.getRules().getColor().hasNoColorsExcept(this.colors)||(this.colors != MagicColor.COLORLESS && card.getRules().getColor().isColorless()))
                    return !this.shouldBeEqual;
            }
            if(colorType!=ColorType.Any)
            {
                switch (colorType)
                {
                    case Colorless:
                        if(!card.getRules().getColor().isColorless())
                            return !this.shouldBeEqual;
                        break;
                    case MonoColor:
                        if(!card.getRules().getColor().isMonoColor())
                            return !this.shouldBeEqual;
                        break;
                    case MultiColor:
                        if(!card.getRules().getColor().isMulticolor())
                            return !this.shouldBeEqual;
                        break;
                }
            }
            if(!this.type.isEmpty())
            {
                boolean found=false;
                for(CardType.CoreType type:card.getRules().getType().getCoreTypes())
                {
                    if(this.type.contains(type))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(!this.superType.isEmpty())
            {
                boolean found=false;
                for(CardType.Supertype type:card.getRules().getType().getSupertypes())
                {
                    if(this.superType.contains(type))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }
            if(this.matchAllSubTypes)
            {
                if(!this.subType.isEmpty())
                {
                    if(this.subType.size()!= Iterables.size(card.getRules().getType().getSubtypes()))
                        return !this.shouldBeEqual;
                    for(String subtype:card.getRules().getType().getSubtypes())
                    {
                        if(!this.subType.contains(subtype))
                        {
                            return !this.shouldBeEqual;
                        }
                    }
                }
            }
            else
            {
                if(!this.subType.isEmpty())
                {
                    boolean found=false;
                    for(String subtype:card.getRules().getType().getSubtypes())
                    {
                        if(this.subType.contains(subtype))
                        {
                            found=true;
                            break;
                        }
                    }
                    if(!found)
                        return !this.shouldBeEqual;
                }
            }

            if(!this.keyWords.isEmpty())
            {
                boolean found=false;
                for(String keyWord:this.keyWords)
                {
                    if(card.getRules().hasKeyword(keyWord))
                    {
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }

            if(!this.deckNeeds.isEmpty())
            {
                boolean found = false;
                for(String need:this.deckNeeds)
                {
                    //FormatExpected: X$Y, where X is DeckHints.Type and Y is a string descriptor
                    String[] parts = need.split("\\$");

                    if (parts.length != 2){
                        continue;
                    }
                    DeckHints.Type t = DeckHints.Type.valueOf(parts[0].toUpperCase());

                    DeckHints hints = card.getRules().getAiHints().getDeckHints();
                    if (hints != null && hints.contains(t, parts[1])){
                        found=true;
                        break;
                    }
                }
                if(!found)
                    return !this.shouldBeEqual;
            }


            return this.shouldBeEqual;
        }
        private Pattern getPattern(RewardData type) {
            if (type.cardText == null || type.cardText.isEmpty())
                return null;
            try {
                return Pattern.compile(type.cardText, Pattern.CASE_INSENSITIVE);
            } catch (Exception e) {
                System.err.println("[" + type.cardName + "|" + type.itemName + "]\n" + e);
                return null;
            }
        }
        public CardPredicate(final RewardData type, final boolean wantEqual) {
            this.matchAllSubTypes=type.matchAllSubTypes;
            this.matchAllColors=type.matchAllColors;
            this.shouldBeEqual = wantEqual;
            for(int i=0;type.manaCosts!=null&&i<type.manaCosts.length;i++)
                manaCosts.add(type.manaCosts[i]);
            text = getPattern(type);
            if(type.colors==null||type.colors.length==0)
            {
                this.colors=MagicColor.ALL_COLORS;
            }
            else
            {
                this.colors=0;
                for(String color:type.colors)
                {
                    if("colorID".equals(color))
                        for (byte c : Current.player().getColorIdentity())
                            colors |= c;
                    else
                        colors |= MagicColor.fromName(color.toLowerCase());
                }
            }
            if(type.keyWords!=null&&type.keyWords.length!=0)
            {
                keyWords.addAll(Arrays.asList(type.keyWords));
            }
            if(type.rarity!=null&&type.rarity.length!=0)
            {
                for(String rarity:type.rarity)
                {
                    rarities.add(CardRarity.smartValueOf(rarity));
                }
            }

            if(type.subTypes!=null&&type.subTypes.length!=0)
            {
                subType.addAll(Arrays.asList(type.subTypes));
            }
            if(type.editions!=null&&type.editions.length!=0)
            {
                editions.addAll(Arrays.asList(type.editions));
            }
            if(type.superTypes!=null&&type.superTypes.length!=0)
            {
                for(String string:type.superTypes)
                    superType.add(CardType.Supertype.getEnum(string));
            }
            if(type.cardTypes!=null&&type.cardTypes.length!=0)
            {
                for(String string:type.cardTypes)
                    this.type.add(CardType.CoreType.getEnum(string));
            }
            if(type.colorType!=null&&!type.colorType.isEmpty())
            {
                this.colorType=ColorType.valueOf(type.colorType);
            }
            else
            {
                this.colorType=ColorType.Any;
            }
            if(type.deckNeeds!=null&&type.deckNeeds.length!=0){
                deckNeeds.addAll(Arrays.asList(type.deckNeeds));
            }
            if(type.minDate!=null&&!type.minDate.isEmpty())
            {
                this.minDate=type.minDate;
            }
            else
            {
                this.minDate="";
            }
        }
    }

    public static List<PaperCard> getPredicateResult(Iterable<PaperCard> cards,final RewardData data)
    {
        List<PaperCard> result = new ArrayList<>();
        CardPredicate pre = new CardPredicate(data, true);

        // Special handling for basic lands to exclude Snow-Covered variants when using cardTypes/subTypes filter
        boolean isBasicLandFilter = false;
        boolean shouldExcludeSnowCovered = false;
        
        if (data.cardTypes != null && data.subTypes != null) {
            for (String cardType : data.cardTypes) {
                if ("Land".equals(cardType)) {
                    for (String subType : data.subTypes) {
                        if ("Plains".equals(subType) || "Island".equals(subType) || 
                            "Swamp".equals(subType) || "Mountain".equals(subType) || 
                            "Forest".equals(subType)) {
                            isBasicLandFilter = true;
                            shouldExcludeSnowCovered = true;
                            break;
                        }
                    }
                }
            }
        }

        for (final PaperCard item : cards)
        {
            if (shouldExcludeSnowCovered && item.getName().startsWith("Snow-Covered")) {
                continue; // Skip Snow-Covered lands when filtering by Land type/subtype
            }
            
            if(pre.test(item))
                result.add(item);
        }
        return result;
    }

    private static boolean isBasicLandName(String name) {
        if (name == null) return false;
        return name.equals("Plains") || name.equals("Island") || name.equals("Swamp") || name.equals("Mountain") || name.equals("Forest") || name.equals("Wastes") || name.startsWith("Snow-Covered");
    }

    public static List<PaperCard> generateCards(Iterable<PaperCard> sourceCardPrintings, final RewardData filterData, final int count, Random randomGenerator)
    {
        // If filterData specifies a basic land by name, ensure sourceCardPrintings includes all its valid printings for diversity.
        if (filterData.cardName != null && isBasicLandName(filterData.cardName)) {
            List<PaperCard> allPrintingsOfBasicLand = FModel.getMagicDb().getCommonCards().getAllCards(filterData.cardName);
            
            ConfigData config = Config.instance().getConfigData();
            final List<String> restrictedEditions = (config != null && config.restrictedEditions != null) ? Arrays.asList(config.restrictedEditions) : Collections.emptyList();
            
            List<PaperCard> globallyAllowedPrintings = allPrintingsOfBasicLand.stream()
                .filter(p -> !restrictedEditions.contains(p.getEdition()))
                .collect(Collectors.toList());

            if (!globallyAllowedPrintings.isEmpty()) {
                sourceCardPrintings = globallyAllowedPrintings;
            }
        }

        final List<PaperCard> resultList = new ArrayList<>();
        
        // Step 1: Filter all printings based on RewardData criteria
        // CardPredicate used by getPredicateResult does not use filterData.cardName, so previous override of sourceCardPrintings is safe.
        List<PaperCard> initialFilteredPrintings = getPredicateResult(sourceCardPrintings, filterData);

        if (initialFilteredPrintings.isEmpty()) {
            return resultList; // No printings match the filter
        }

        // Step 2: Extract unique card names from the filtered printings
        List<String> uniqueCardNameList = initialFilteredPrintings.stream()
                                                                 .map(PaperCard::getName)
                                                                 .distinct()
                                                                 .collect(Collectors.toList());

        if (uniqueCardNameList.isEmpty()) {
            return resultList; // Should not happen if initialFilteredPrintings is not empty, but as a safeguard
        }

        // Step 3: Loop 'count' times to select cards
        for (int i = 0; i < count; i++) {
            if (uniqueCardNameList.isEmpty()) break; // Cannot select more unique names if list is exhausted (though we allow name repeats)

            // Step 3a: Select a Unique Name randomly
            // If sourceCardPrintings was overridden for a basic land, uniqueCardNameList will contain just that one basic land name.
            String chosenName = uniqueCardNameList.get(randomGenerator.nextInt(uniqueCardNameList.size()));

            // Step 3b: Gather all printings of chosenName that were in initialFilteredPrintings
            List<PaperCard> candidatePrintingsForChosenName = initialFilteredPrintings.stream()
                                                                                      .filter(pc -> pc.getName().equals(chosenName))
                                                                                      .collect(Collectors.toList());
            
            // Step 3c: Handle empty candidates (should be rare if logic is correct)
            if (candidatePrintingsForChosenName.isEmpty()) {
                System.err.println("CardUtil.generateCards: No candidate printings found for chosen name '" + chosenName + "' from initial filtered list. Skipping this selection.");
                continue;
            }

            // Step 3d: Select a Random Printing from the candidates for the chosen name
            PaperCard finalChosenPrinting = candidatePrintingsForChosenName.get(randomGenerator.nextInt(candidatePrintingsForChosenName.size()));

            // Step 3e: Add the selected printing to the result list
            resultList.add(finalChosenPrinting);
        }
        
        return resultList;
    }

// New helper method in CardUtil.java
    private static Deck filterDeckByRestrictedEditions(Deck deckToFilter) {
        if (deckToFilter == null) {
            return null;
        }
        ConfigData configData = Config.instance().getConfigData();
        if (configData == null || configData.restrictedEditions == null || configData.restrictedEditions.length == 0) {
            return deckToFilter; // No restrictions to apply
        }
        List<String> restrictedEditionsList = Arrays.asList(configData.restrictedEditions);
        List<PaperCard> cardsToRemove = new ArrayList<>();

        // Check main deck
        // Use a null-safe way to get the main deck section
        CardPool mainDeckPool = deckToFilter.get(DeckSection.Main); // Changed type to CardPool
        if (mainDeckPool != null) {
            // Iterate over entries (PaperCard -> count) in the CardPool
            for (Map.Entry<PaperCard, Integer> entry : mainDeckPool) { // Assuming CardPool is Iterable<Map.Entry<PaperCard, Integer>>
                PaperCard card = entry.getKey();
                if (restrictedEditionsList.contains(card.getEdition())) {
                    // Add all copies of the card to be removed
                    for (int i = 0; i < entry.getValue(); i++) {
                        cardsToRemove.add(card);
                    }
                }
            }
            for (PaperCard cardToRemove : cardsToRemove) {
                mainDeckPool.remove(cardToRemove);
            }
            cardsToRemove.clear();
        }

        // Check sideboard
        // Use a null-safe way to get the sideboard section
        CardPool sideBoardPool = deckToFilter.get(DeckSection.Sideboard); // Changed type to CardPool
        if (sideBoardPool != null) { // Check if sideboard exists and is not null
            // Iterate over entries (PaperCard -> count) in the CardPool
            for (Map.Entry<PaperCard, Integer> entry : sideBoardPool) { // Assuming CardPool is Iterable<Map.Entry<PaperCard, Integer>>
                PaperCard card = entry.getKey();
                if (restrictedEditionsList.contains(card.getEdition())) {
                    // Add all copies of the card to be removed
                    for (int i = 0; i < entry.getValue(); i++) {
                        cardsToRemove.add(card);
                    }
                }
            }
            for (PaperCard cardToRemove : cardsToRemove) {
                sideBoardPool.remove(cardToRemove);
            }
        }
        return deckToFilter;
    }
    public static int getCardPrice(PaperCard card)
    {
        if(card==null)
            return 0;
        switch (card.getRarity())
        {
            case BasicLand:
                return 5;
            case Common:
                return 50;
            case Uncommon:
                return 150;
            case Rare:
                return 300;
            case MythicRare:
                return 500;
            default:
                return 600;
        }
    }
    public static int getRewardPrice(Reward reward)
    {
        PaperCard card=reward.getCard();
        if(card!=null)
            return getCardPrice(card);
        if(reward.getItem()!=null)
            return reward.getItem().cost;
        if(reward.getType()== Reward.Type.Life)
            return reward.getCount()*500;
        if(reward.getType()== Reward.Type.Shards)
            return reward.getCount()*500;
        if(reward.getType()== Reward.Type.Gold)
            return reward.getCount();
        /*if(reward.getType() == Reward.Type.CardPack)
            return reward.getDeck().get(DeckSection.Main).countAll()*70;*/
        //TODO: Heitor - Price by card count and type of boosterPack.


        return 1000;
    }

    public static Deck generateDeck(GeneratedDeckData data, CardEdition starterEdition, boolean discourageDuplicates)
    {
        List<String> editionCodes = (starterEdition != null)?Arrays.asList(starterEdition.getCode(), starterEdition.getCode2()):Arrays.asList("JMP", "J22", "DMU", "BRO", "ONE", "MOM");
        Deck deck= new Deck(data.name);
        if(data.mainDeck!=null)
        {
            deck.getOrCreate(DeckSection.Main).addAllFlat(generateAllCards(Arrays.asList(data.mainDeck), true));
            if(data.sideBoard!=null)
                deck.getOrCreate(DeckSection.Sideboard).addAllFlat(generateAllCards(Arrays.asList(data.sideBoard), true));
            return deck;
        }
        if(data.jumpstartPacks!=null)
        {
            deck.getOrCreate(DeckSection.Main);

            Map <String, List<PaperCard>> packCandidates=null;
            List<String> usedPackNames=new ArrayList<String>();

            for(int i=0;i<data.jumpstartPacks.length;i++)
            {

                final byte targetColor = MagicColor.fromName(data.jumpstartPacks[i]);
                String targetName;
                switch (targetColor)
                {
                    default:
                    case MagicColor.WHITE:  targetName = "Plains";  break;
                    case MagicColor.BLUE:   targetName = "Island";  break;
                    case MagicColor.BLACK:  targetName = "Swamp";   break;
                    case MagicColor.RED:    targetName = "Mountain";break;
                    case MagicColor.GREEN:  targetName = "Forest";  break;
                }

                packCandidates=new HashMap<>();
                ConfigData configData = Config.instance().getConfigData(); // Get config data
                List<String> restrictedEditionsList = Arrays.asList(configData.restrictedEditions); // Cache restricted editions

                for(SealedTemplate template : StaticData.instance().getSpecialBoosters())
                {
                    String templateEditionCode = template.getEdition().split("\\s",2)[0];
                    if (!editionCodes.contains(templateEditionCode)) // Original check
                        continue;
                    if (restrictedEditionsList.contains(templateEditionCode)) // New check for restricted editions
                        continue;
                    List<PaperCard> packContents = new UnOpenedProduct(template).get();
                    if (packContents.size() < 18 | packContents.size() > 25)
                        continue;
                    if (packContents.stream().filter(x -> x.getName().equals(targetName)).count() >=3)
                        packCandidates.putIfAbsent(template.getEdition(), packContents);
                }
                List<PaperCard> selectedPack;
                if (discourageDuplicates) {
                    Map <String, List<PaperCard>> filteredPackCandidates= new HashMap<>();
                    for (java.util.Map.Entry<String, List<PaperCard>>  entry: packCandidates.entrySet()){
                        if (!usedPackNames.contains(entry.getKey())){
                            filteredPackCandidates.put(entry.getKey(), entry.getValue()); //deep copy so that packCandidates can be used if filtered ends up being empty
                        }
                    }
                    //Only re-use a pack if all possibilities have already been chosen
                    if (filteredPackCandidates.size() == 0)
                        filteredPackCandidates = packCandidates;
                    Object[] keys = filteredPackCandidates.keySet().toArray();

                    String keyName = (String)keys[Current.world().getRandom().nextInt(keys.length)];
                    usedPackNames.add(keyName);
                    selectedPack = filteredPackCandidates.remove(keyName);
                }
                else{
                    Object[] keys = packCandidates.keySet().toArray();
                    selectedPack = packCandidates.get((String)keys[Current.world().getRandom().nextInt(keys.length)]);
                }
                //if the packContents size above is below 20, just get random card
                int size = 20 - selectedPack.size();
                for (int c = 0; c < size; c++) {
                    selectedPack.add(Aggregates.random(selectedPack));
                }
                deck.getOrCreate(DeckSection.Main).addAllFlat(selectedPack);
            }
            return deck;
        }
       if(data.template!=null)
       {
           float count=data.template.count;
           float lands=count*0.4f;
           float spells=count-lands;
           List<RewardData> dataArray= generateRewards(data.template,spells*0.5f,new int[]{1,2});
           dataArray.addAll(generateRewards(data.template,spells*0.3f,new int[]{3,4,5}));
           dataArray.addAll(generateRewards(data.template,spells*0.2f,new int[]{6,7,8}));
           List<PaperCard>  nonLand= generateAllCards(dataArray, true);

           nonLand.addAll(fillWithLands(nonLand,data.template));
           deck.getOrCreate(DeckSection.Main).addAllFlat(nonLand);
       }
        return deck;
    }

    private static List<PaperCard> fillWithLands(List<PaperCard> nonLands, GeneratedDeckTemplateData template) {
        int red=0;
        int blue=0;
        int green=0;
        int white=0;
        int black=0;
        int colorLess=0;
        int cardCount=nonLands.size();
        List<PaperCard> cards=new ArrayList<>();
        boolean allCardVariants=Config.instance().getSettingData().useAllCardVariants;

        for(PaperCard nonLand:nonLands)
        {
            red+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.RED);
            green+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.GREEN);
            white+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.WHITE);
            blue+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.BLUE);
            black+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.BLACK);
            colorLess+=nonLand.getRules().getManaCost().getShardCount(ManaCostShard.GENERIC);
        }
        float sum= red+ blue+ green+ white+ black;
        int neededLands=template.count-cardCount;
        int neededDualLands= Math.round (neededLands*template.rares);
        int neededBase=neededLands-neededDualLands;
        String edition = "";
        if (allCardVariants) {
            PaperCard templateLand = CardUtil.getCardByName("Plains");
            edition = templateLand.getEdition();
        }
        if(sum==0.)
        {
            cards.addAll(generateLands("Wastes",neededLands));
        }
        else
        {
            int mount=Math.round(neededBase*(red/sum));
            int island=Math.round(neededBase*(blue/sum));
            int forest=Math.round(neededBase*(green/sum));
            int plains=Math.round(neededBase*(white/sum));
            int swamp=Math.round(neededBase*(black/sum));
            cards.addAll(generateLands("Plains",plains,edition));
            cards.addAll(generateLands("Island",island,edition));
            cards.addAll(generateLands("Forest",forest,edition));
            cards.addAll(generateLands("Mountain",mount,edition));
            cards.addAll(generateLands("Swamp",swamp,edition));
            List<String> landTypes=new ArrayList<>();
            if(mount>0)
                landTypes.add("Mountain");
            if(island>0)
                landTypes.add("Island");
            if(plains>0)
                landTypes.add("Plains");
            if(swamp>0)
                landTypes.add("Swamp");
            if(forest>0)
                landTypes.add("Forest");
            cards.addAll(generateDualLands(landTypes,neededDualLands));

        }
        return cards;
    }

    private static Collection<PaperCard> generateDualLands(List<String> landName, int count) {
        ArrayList<RewardData> rewards=new ArrayList<>();
        RewardData base= new RewardData();
        rewards.add(base);
        base.cardTypes=new String[]{"Land"};
        base.count=count;
        base.matchAllSubTypes=true;
        if(landName.size()==1)
        {
            base.subTypes=new String[]{landName.get(0)};
        }
        else if(landName.size()==2)
        {
            base.subTypes=new String[]{landName.get(0),landName.get(1)};
        }
        else if(landName.size()==3)
        {
            RewardData sub1= new RewardData(base);
            RewardData sub2= new RewardData(base);
            sub1.count/=3;
            sub2.count/=3;
            base.count-=sub1.count;
            base.count-=sub2.count;

            base.subTypes=new String[]{landName.get(0),landName.get(1)};
            sub1.subTypes=new String[]{landName.get(1),landName.get(2)};
            sub2.subTypes=new String[]{landName.get(0),landName.get(2)};
            rewards.addAll(Arrays.asList(sub1,sub2));
        }
        else if(landName.size()==4)
        {
            RewardData sub1= new RewardData(base);
            RewardData sub2= new RewardData(base);
            RewardData sub3= new RewardData(base);
            RewardData sub4= new RewardData(base);
            sub1.count/=5;
            sub2.count/=5;
            sub3.count/=5;
            sub4.count/=5;
            base.count-=sub1.count;
            base.count-=sub2.count;
            base.count-=sub3.count;
            base.count-=sub4.count;

            base.subTypes = new String[]{landName.get(0),landName.get(1)};
            sub1.subTypes = new String[]{landName.get(0),landName.get(2)};
            sub2.subTypes = new String[]{landName.get(0),landName.get(3)};
            sub3.subTypes = new String[]{landName.get(1),landName.get(2)};
            sub4.subTypes = new String[]{landName.get(1),landName.get(3)};
            rewards.addAll(Arrays.asList(sub1,sub2,sub3,sub4));
        }
        else if(landName.size()==5)
        {
            RewardData sub1= new RewardData(base);
            RewardData sub2= new RewardData(base);
            RewardData sub3= new RewardData(base);
            RewardData sub4= new RewardData(base);
            RewardData sub5= new RewardData(base);
            RewardData sub6= new RewardData(base);
            RewardData sub7= new RewardData(base);
            RewardData sub8= new RewardData(base);
            RewardData sub9= new RewardData(base);
            sub1.count/=10;
            sub2.count/=10;
            sub3.count/=10;
            sub4.count/=10;
            sub5.count/=10;
            sub6.count/=10;
            sub7.count/=10;
            sub8.count/=10;
            sub9.count/=10;
            base.count-=sub1.count;
            base.count-=sub2.count;
            base.count-=sub3.count;
            base.count-=sub4.count;
            base.count-=sub5.count;
            base.count-=sub6.count;
            base.count-=sub7.count;
            base.count-=sub8.count;
            base.count-=sub9.count;

            base.subTypes=new String[]{landName.get(0),landName.get(1)};
            sub1.subTypes=new String[]{landName.get(0),landName.get(2)};
            sub2.subTypes=new String[]{landName.get(0),landName.get(3)};
            sub3.subTypes=new String[]{landName.get(0),landName.get(4)};
            sub4.subTypes=new String[]{landName.get(1),landName.get(2)};
            sub5.subTypes=new String[]{landName.get(1),landName.get(3)};
            sub6.subTypes=new String[]{landName.get(1),landName.get(4)};
            sub7.subTypes=new String[]{landName.get(2),landName.get(3)};
            sub8.subTypes=new String[]{landName.get(2),landName.get(4)};
            sub9.subTypes=new String[]{landName.get(3),landName.get(4)};
            rewards.addAll(Arrays.asList(sub1,sub2,sub3,sub4,sub5,sub6,sub7,sub8,sub9));
        }

        Collection<PaperCard> ret = new ArrayList<>(generateAllCards(rewards, true));
        return ret;
    }

    private static Collection<PaperCard> generateLands(String landName, int count) {
        return generateLands(landName, count, "");
    }

    private static Collection<PaperCard> generateLands(String landName, int count, String edition) {
        boolean allCardVariants = Config.instance().getSettingData().useAllCardVariants;
        Collection<PaperCard> ret = new ArrayList<>();

        if (allCardVariants) {
            if (edition.isEmpty()) {
                PaperCard templateLand = getCardByName(landName);
                edition = templateLand.getEdition();
            }
            for (int i = 0; i < count; i++) {
                ret.add(getCardByNameAndEdition(landName, edition));
            }
        } else {
            for (int i = 0; i < count; i++)
                ret.add(FModel.getMagicDb().getCommonCards().getCard(landName));
        }

        return ret;
    }

    private static List<RewardData> generateRewards(GeneratedDeckTemplateData template, float count, int[] manaCosts) {
        ArrayList<RewardData> ret=new ArrayList<>();
        ret.addAll(templateGenerate(template,count-(count*template.rares),manaCosts,new String[]{"Uncommon","Common"}));
        ret.addAll(templateGenerate(template,count*template.rares,manaCosts,new String[]{"Rare","Mythic Rare"}));
        return ret;
    }

    private static ArrayList<RewardData> templateGenerate(GeneratedDeckTemplateData template, float count, int[] manaCosts, String[] strings) {
        ArrayList<RewardData> ret=new ArrayList<>();
        RewardData base= new RewardData();
        base.manaCosts=manaCosts;
        base.rarity=strings;
        base.colors=template.colors;
        if(template.tribe!=null&&!template.tribe.isEmpty())
        {
            RewardData caresAbout= new RewardData(base);
            caresAbout.cardText="\\b"+template.tribe+"\\b";
            caresAbout.count=  Math.round(count*template.tribeSynergyCards);
            ret.add(caresAbout);

            base.subTypes=new String[]{template.tribe};
            base.count=  Math.round(count*(1-template.tribeSynergyCards));
        }
        else
        {
            base.count=  Math.round(count);
        }
        ret.add(base);
        return  ret;
    }

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme, boolean useGeneticAI) {
        return getDeck(path, forAI, isFantasyMode, colors, isTheme, useGeneticAI, null,true);
    }

    public static Deck getDeck(String path, boolean forAI, boolean isFantasyMode, String colors, boolean isTheme, boolean useGeneticAI, CardEdition starterEdition, boolean discourageDuplicates)
    {
        if(path.endsWith(".dck")) {
            FileHandle fileHandle = Config.instance().getFile(path);
            Deck deck = null;
            if (fileHandle != null) {
                deck = DeckSerializer.fromFile(fileHandle.file());
            }
            if (deck == null) {
                deck = filterDeckByRestrictedEditions(DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, true, false, true));
                System.err.println("Error loading Deck: " + path + "\nGenerating random deck: " + (deck != null ? deck.getName() : "null deck after filtering"));
            }
            return filterDeckByRestrictedEditions(deck); // Also filter the successfully loaded .dck file as a precaution
        }

        if(forAI && (isFantasyMode||useGeneticAI)) {
            Deck deck = filterDeckByRestrictedEditions(DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, forAI, isTheme, useGeneticAI));
            if (deck != null)
                return deck; // Already filtered
        }
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(path);
        if (handle.exists())
            return generateDeck(json.fromJson(GeneratedDeckData.class, handle), starterEdition, discourageDuplicates); // generateDeck (JSON template) is assumed to be compliant now
        Deck deck = filterDeckByRestrictedEditions(DeckgenUtil.getRandomOrPreconOrThemeDeck(colors, true, false, true));
        System.err.println("Error loading JSON: " + handle.path() + "\nGenerating random deck: "+ (deck != null ? deck.getName() : "null deck after filtering"));
        return deck; // Already filtered

    }

    private static final GameFormat.Collection  formats   = FModel.getFormats();





    private static final Predicate<CardEdition> filterPioneer = formats.getPioneer().editionLegalPredicate;
    private static final Predicate<CardEdition> filterModern= formats.getModern().editionLegalPredicate;
    private static final Predicate<CardEdition> filterVintage = formats.getVintage().editionLegalPredicate;
    private static final Predicate<CardEdition> filterStandard = formats.getStandard().editionLegalPredicate;
    public static Deck generateStandardBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterStandard);
    }
    public static Deck generatePioneerBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterPioneer);
    }
    public static Deck generateModernBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterModern);
    }
    public static Deck generateVintageBoosterAsDeck(){
        return generateRandomBoosterPackAsDeck(filterVintage);
    }

    public static Deck generateBoosterPackAsDeck(String code){
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedEditions != null) {
            if (!Arrays.asList(configData.allowedEditions).contains(code)){
                System.err.println("Cannot generate booster pack, '" + code + "' is not an allowed edition");
            }
        } else if (Arrays.asList(configData.restrictedEditions).contains(code)){
            System.err.println("Cannot generate booster pack, '" + code + "' is a restricted edition");
        }

        CardEdition edition = StaticData.instance().getEditions().get(code);
        if (edition == null){
            System.err.println("Set code '" + code + "' not found.");
            return new Deck();
        }
        BoosterPack cards = BoosterPack.fromSet(edition);
        return generateBoosterPackAsDeck(edition);
    }

    public static Deck generateBoosterPackAsDeck(CardEdition edition){
        Deck d = new Deck("Booster pack");
        d.setComment(edition.getCode());
        d.getMain().add(BoosterPack.fromSet(edition).getCards());
        return d;
    }

    public static Deck generateRandomBoosterPackAsDeck(final Predicate<CardEdition> editionFilter) {
        Predicate<CardEdition> filter = CardEdition.Predicates.CAN_MAKE_BOOSTER.and(editionFilter);
        Iterable<CardEdition> possibleEditions = IterableUtil.filter(FModel.getMagicDb().getEditions(), filter);

        if (!possibleEditions.iterator().hasNext()) {
            System.err.println("No sets found matching edition filter that can create boosters.");
            return null;
        }

        CardEdition edition = Aggregates.random(possibleEditions);
        return generateBoosterPackAsDeck(edition);
    }

    public static PaperCard getCardByName(String cardName) {
        List<PaperCard> validCards;
        
        // Special handling for basic lands to show diverse printings
        boolean isBasicLand = isBasicLandName(cardName);
        if (isBasicLand) {
            // For basic lands, get all printings but filter based on config restrictions
            validCards = FModel.getMagicDb().getCommonCards().getAllCards(cardName);
            // Filter to allowedEditions or remove restrictedEditions
            ConfigData configData = Config.instance().getConfigData();
            if (configData != null) {
                if (configData.allowedEditions != null && configData.allowedEditions.length > 0) {
                    List<String> allowedList = Arrays.asList(configData.allowedEditions);
                    validCards = validCards.stream()
                            .filter(card -> allowedList.contains(card.getEdition()))
                            .collect(Collectors.toList());
                } else if (configData.restrictedEditions != null && configData.restrictedEditions.length > 0) {
                    List<String> restrictedList = Arrays.asList(configData.restrictedEditions);
                    validCards = validCards.stream()
                            .filter(card -> !restrictedList.contains(card.getEdition()))
                            .collect(Collectors.toList());
                }
            }
            // If no valid cards after filtering, fall back to default behavior
            if (validCards.isEmpty()) {
                validCards = FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt(cardName);
            }
        } else {
            //Standard behavior for non-basic land cards
            if (Config.instance().getSettingData().useAllCardVariants)
                validCards = FModel.getMagicDb().getCommonCards().getAllCards(cardName);
            else
                validCards = FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt(cardName);
        }
        
        // Use a new Random instance to ensure printing diversity isn't tied to the world's seeded RNG
        return validCards.get(new Random().nextInt(validCards.size()));
    }

    public static PaperCard getCardByNameAndEdition(String cardName, String edition) {
        List<PaperCard> cardPool = Config.instance().getSettingData().useAllCardVariants
                ? FModel.getMagicDb().getCommonCards().getAllCards(cardName)
                : FModel.getMagicDb().getCommonCards().getUniqueCardsNoAlt(cardName);
        List<PaperCard> validCards = cardPool.stream()
                .filter(input -> input.getEdition().equals(edition)).collect(Collectors.toList());

        if (validCards.isEmpty()) {
            System.err.println("Unexpected behavior: tried to call getCardByNameAndEdition for card " + cardName + " from the edition " + edition + ", but didn't find it in the DB. A random existing instance will be returned.");
            // Fallback still uses the potentially diverse getCardByName
            return getCardByName(cardName);
        }

        // Use a new Random instance here as well for consistency
        return validCards.get(new Random().nextInt(validCards.size()));
    }

    public static Collection<PaperCard> getFullCardPool(boolean allCardVariants) {
        // Always return all cards initially; filtering by edition happens later.
        // The allCardVariants flag might be used elsewhere, but for initial pool generation, we need all printings.
        return FModel.getMagicDb().getCommonCards().getAllCards();
    }
}

