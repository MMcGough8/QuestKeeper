package com.questkeeper;

import com.questkeeper.core.GameEngine;

/**
 * QuestKeeper - A CLI-based D&D 5e Auto DM Adventure Game
 *
 * Entry point for the application.
 *
 * @author Marc McGough
 * @version 1.0.0
 */
public class Main {

    public static void main(String[] args) {
        GameEngine engine = new GameEngine();
        engine.start();
    }
}