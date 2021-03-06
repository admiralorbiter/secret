package Gloomhaven.AttackModifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AttackModifierDeck {
	
	List<AttackModifierCard> deck = new ArrayList<AttackModifierCard>();
	
	public AttackModifierDeck(String id){
		switch(id) {
			case "Standard":
				for(int i=0; i<6; i++)
					deck.add(new AttackModifierCard(1, 0));
				
				for(int i=0; i<5; i++) {
					deck.add(new AttackModifierCard(1, 1));
					deck.add(new AttackModifierCard(1, -1));
				}
				
				deck.add(new AttackModifierCard(1, 2));
				deck.add(new AttackModifierCard(1, -2));
				deck.add(new AttackModifierCard(1, 1));
				deck.add(new AttackModifierCard(2, 0, true, false));
				deck.add(new AttackModifierCard(1, 0, true, false));
				break;
			default:
				for(int i=0; i<6; i++)
					deck.add(new AttackModifierCard(1, 0));
				
				for(int i=0; i<5; i++) {
					deck.add(new AttackModifierCard(1, 1));
					deck.add(new AttackModifierCard(1, -1));
				}
				
				deck.add(new AttackModifierCard(1, 2));
				deck.add(new AttackModifierCard(1, -2));
				deck.add(new AttackModifierCard(1, 1));
				deck.add(new AttackModifierCard(2, 0, true, false));
				deck.add(new AttackModifierCard(1, 0, true, false));
		}
	}

	
	public AttackModifierCard pickRandomModifierCard() {
		Random rand = new Random();
		boolean running=true;
		do
		{
		 int pick = rand.nextInt(deck.size());
		 if(deck.get(pick).cardFree()) {
			 deck.get(pick).discard();
			 if(deck.get(pick).getShuffle())
				 shuffleDeck();
			 
			 return deck.get(pick);
		 }
		}
		while(running);
		return null;//should never get here.
	}
	
	public void shuffleDeck() {
		for(int i=0; i<deck.size(); i++)
			deck.get(i).setFree();
	}
	
	public void addCard(AttackModifierCard card) {
		deck.add(card);
	}
	
	public boolean firstIsBest(AttackModifierCard card, AttackModifierCard secondCard) {

		if(secondCard.getMultiplier()>card.getMultiplier())
			return false;
		
		if(secondCard.getPlusAttack()>card.getPlusAttack())
			return false;
		
		return true;
	}
	
	public void remove(AttackModifierCard card) {
		deck.remove(card);
	}

}
