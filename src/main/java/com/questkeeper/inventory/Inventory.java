package com.questkeeper.inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manages a collection of items with weight limits and equipment slots.
 * 
 * Handles inventory operations like adding, removing, and equipping items.
 * Tracks carrying capacity based on character strength.
 * 
 * @author Marc McGough
 * @version 1.0
 */

public class Inventory {
