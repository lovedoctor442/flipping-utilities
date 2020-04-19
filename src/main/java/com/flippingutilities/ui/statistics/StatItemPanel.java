/*
 * Copyright (c) 2020, Belieal <https://github.com/Belieal>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.flippingutilities.ui.statistics;

import com.flippingutilities.FlippingItem;
import com.flippingutilities.FlippingPlugin;
import com.flippingutilities.OfferInfo;
import com.flippingutilities.ui.UIUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.AsyncBufferedImage;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
public class StatItemPanel extends JPanel
{
	private static final Border ITEM_INFO_BORDER = new CompoundBorder(
		BorderFactory.createLineBorder(ColorScheme.DARKER_GRAY_COLOR.darker(), 3),
		BorderFactory.createEmptyBorder(3, 3, 3, 3));

	private static final Border TRADE_HISTORY_BORDER = new CompoundBorder(
		BorderFactory.createMatteBorder(0, 0, 2, 0, ColorScheme.LIGHT_GRAY_COLOR),
		BorderFactory.createEmptyBorder(3, 5, 3, 5));

	private FlippingPlugin plugin;
	private ScheduledExecutorService executor;
	private FlippingItem flippingItem;

	private StatsPanel statsPanel;

	private long totalProfit;
	private long totalExpense;
	private long totalRevenue;
	private int itemCountFlipped;
	private Instant startOfInterval;
	private ArrayList<OfferInfo> tradeHistory = new ArrayList<>();

	/*
	 Panels that construct the title panel that contains
	 identifying information about the item.
	 */
	//Holds the item icon, name, item profit labels and collapse icon
	private JPanel titlePanel = new JPanel(new BorderLayout());
	//Holds the name and item profit labels
	private JPanel nameAndProfitTitlePanel = new JPanel(new BorderLayout());

	//Shows the name label for the item
	private JLabel nameTitleLabel = new JLabel();
	//Shows the item's profit
	private JLabel itemProfitTitleLabel = new JLabel();

	//Shows the item's icon
	private JLabel itemIconTitleLabel = new JLabel();
	//Label that controls the collapse function of the item panel.
	private JLabel collapseIconTitleLabel = new JLabel();

	//Contains the sub info container and trade history panel.
	private JPanel subInfoAndHistoryContainer = new JPanel(new BorderLayout());

	private JLabel totalProfitValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel profitEachValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel timeOfLastFlipValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel quantityValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel roiValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel avgBuyPriceValLabel = new JLabel("", SwingConstants.RIGHT);
	private JLabel avgSellPriceValLabel = new JLabel("", SwingConstants.RIGHT);

	/* These panels contain the sub information regarding the item.
	   Subinfos are general statistics about an item over the time interval currently selected. */

	private JPanel totalProfitPanel = new JPanel(new BorderLayout());
	private JPanel profitEachPanel = new JPanel(new BorderLayout());
	private JPanel timeOfLastFlipPanel = new JPanel(new BorderLayout());
	private JPanel quantityPanel = new JPanel(new BorderLayout());
	private JPanel padPanel = new JPanel(new BorderLayout());
	private JPanel roiPanel = new JPanel(new BorderLayout());
	private JPanel avgBuyPricePanel = new JPanel(new BorderLayout());
	private JPanel avgSellPricePanel = new JPanel(new BorderLayout());

	private final JPanel[] subInfoPanelArray = {totalProfitPanel, profitEachPanel, timeOfLastFlipPanel, quantityPanel,
		padPanel, roiPanel, avgBuyPricePanel, avgSellPricePanel};

	/* Trade History containers. */
	//Wraps the title label panel and the item history container.
	private JPanel tradeHistoryPanel = new JPanel(new BorderLayout());
	private JPanel tradeHistoryTitlePanel = new JPanel(new BorderLayout());
	//Holds the individual trades in the history.
	private JPanel tradeHistoryItemContainer = new JPanel(new GridBagLayout());

	private JLabel collapseTradeHistoryIconLabel = new JLabel(UIUtilities.CLOSE_ICON);

	//Constraints for tradeHistoryItemContainer.
	private GridBagConstraints constraints = new GridBagConstraints();

	/**
	 * This panel represents the middle layer of information. It contains general information about the item
	 * along with being the container for the trade history of that item.
	 *
	 * @param plugin       Used to access the plugin user config.
	 * @param itemManager  Used to get the icon of the item.
	 * @param executor     Used to allow for periodic updates of the panel if need be.
	 * @param flippingItem The item that the panel represents.
	 */

	StatItemPanel(FlippingPlugin plugin, ItemManager itemManager, ScheduledExecutorService executor, FlippingItem flippingItem)
	{
		this.plugin = plugin;
		this.executor = executor;
		this.flippingItem = flippingItem;

		setLayout(new BorderLayout());

		//Get parent
		statsPanel = plugin.getStatPanel();

		updateDisplays();

		/* Item icon */
		AsyncBufferedImage itemImage = itemManager.getImage(flippingItem.getItemId());
		Runnable resize = () ->
		{
			BufferedImage subIcon = itemImage.getSubimage(0, 0, 32, 32);
			itemIconTitleLabel.setIcon(new ImageIcon(subIcon.getScaledInstance(24, 24, Image.SCALE_SMOOTH)));
		};
		itemImage.onLoaded(resize);
		resize.run();
		itemIconTitleLabel.setBorder(new EmptyBorder(0, 2, 0, 5));

		/* Item name and profit label */
		nameAndProfitTitlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		nameAndProfitTitlePanel.add(nameTitleLabel, BorderLayout.NORTH);
		nameAndProfitTitlePanel.add(itemProfitTitleLabel, BorderLayout.SOUTH);

		/* Collapse icon */
		collapseIconTitleLabel.setIcon(flippingItem.isShouldCollapseStatItem() ? UIUtilities.CLOSE_ICON : UIUtilities.OPEN_ICON);
		collapseIconTitleLabel.setBorder(new EmptyBorder(2, 2, 2, 2));
		titlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (subInfoAndHistoryContainer.isVisible())
					{
						collapseIconTitleLabel.setIcon(UIUtilities.CLOSE_ICON);
						subInfoAndHistoryContainer.setVisible(false);
						flippingItem.setShouldCollapseStatItem(false);
					}
					else
					{
						collapseIconTitleLabel.setIcon(UIUtilities.OPEN_ICON);
						subInfoAndHistoryContainer.setVisible(true);
						flippingItem.setShouldCollapseStatItem(true);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				nameAndProfitTitlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
				titlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				nameAndProfitTitlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
				titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
			}
		});

		titlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
		titlePanel.setBorder(new EmptyBorder(2, 2, 2, 2));

		titlePanel.add(itemIconTitleLabel, BorderLayout.WEST);
		titlePanel.add(nameAndProfitTitlePanel, BorderLayout.CENTER);
		titlePanel.add(collapseIconTitleLabel, BorderLayout.EAST);

		subInfoAndHistoryContainer.setVisible(flippingItem.isShouldCollapseStatItem());

		/* Item sub infos */
		/* Main subinfo name and value labels */
		//Using arrays to make it easier to set UI looks en masse
		JLabel[] textLabelArray = {new JLabel("Total Profit: "), new JLabel("Avg. Profit ea: "), new JLabel("Last Traded: "), new JLabel("Quantity Flipped: "),
			new JLabel(" "), new JLabel("Avg. ROI: "), new JLabel("Avg. Buy Price: "), new JLabel("Avg. Sell Price: ")};

		JLabel[] valLabelArray = {totalProfitValLabel, profitEachValLabel, timeOfLastFlipValLabel, quantityValLabel,
			new JLabel(" "), roiValLabel, avgBuyPriceValLabel, avgSellPriceValLabel};

		JPanel subInfoContainer = new JPanel();
		subInfoContainer.setLayout(new DynamicGridLayout(valLabelArray.length, textLabelArray.length));

		boolean useAltColor = false;
		for (int i = 0; i < subInfoPanelArray.length; i++)
		{
			JLabel textLabel = textLabelArray[i];
			JLabel valLabel = valLabelArray[i];
			JPanel panel = subInfoPanelArray[i];

			panel.add(textLabel, BorderLayout.WEST);
			panel.add(valLabel, BorderLayout.EAST);

			panel.setBorder(new EmptyBorder(4, 2, 4, 2));

			panel.setBackground(useAltColor ? UIUtilities.DARK_GRAY_ALT_ROW_COLOR : ColorScheme.DARKER_GRAY_COLOR);
			useAltColor = !useAltColor;

			textLabel.setForeground(ColorScheme.GRAND_EXCHANGE_ALCH);

			textLabel.setFont(FontManager.getRunescapeSmallFont());
			valLabel.setFont(FontManager.getRunescapeSmallFont());

			subInfoContainer.add(panel);
		}

		//Set font colors of right value labels
		timeOfLastFlipValLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		profitEachValLabel.setForeground(ColorScheme.GRAND_EXCHANGE_PRICE);
		quantityValLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		avgBuyPriceValLabel.setForeground(UIUtilities.PROFIT_COLOR);
		avgSellPriceValLabel.setForeground(UIUtilities.PROFIT_COLOR);

		/* Trade History (NYI) */
		//Shows the individual flips made for the item.
		tradeHistoryTitlePanel.setBorder(TRADE_HISTORY_BORDER);

		JLabel tradeHistoryTitleLabel = new JLabel("Trade History", SwingConstants.CENTER);
		tradeHistoryTitlePanel.add(tradeHistoryTitleLabel, BorderLayout.CENTER);
		tradeHistoryTitlePanel.add(collapseTradeHistoryIconLabel, BorderLayout.EAST);
		tradeHistoryTitlePanel.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				if (e.getButton() == MouseEvent.BUTTON1)
				{
					if (subInfoAndHistoryContainer.isVisible())
					{
						tradeHistoryItemContainer.setVisible(false);
						collapseTradeHistoryIconLabel.setIcon(UIUtilities.CLOSE_ICON);
					}
					else
					{
						tradeHistoryItemContainer.setVisible(true);
						collapseTradeHistoryIconLabel.setIcon(UIUtilities.OPEN_ICON);
					}
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				tradeHistoryTitlePanel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				tradeHistoryTitlePanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());
			}
		});

		tradeHistoryPanel.add(tradeHistoryTitlePanel, BorderLayout.NORTH);
		tradeHistoryPanel.add(tradeHistoryItemContainer, BorderLayout.CENTER);
		tradeHistoryPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR.darker());


		//Set background and border of container with sub infos and trade history
		subInfoAndHistoryContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		subInfoAndHistoryContainer.setBorder(ITEM_INFO_BORDER);

		subInfoAndHistoryContainer.add(subInfoContainer, BorderLayout.CENTER);
		//subInfoAndHistoryContainer.add(tradeHistoryPanel, BorderLayout.SOUTH);

		add(titlePanel, BorderLayout.NORTH);
		add(subInfoAndHistoryContainer, BorderLayout.CENTER);
	}

	/**
	 * Updates the values that determine what is shown on the panel along with updating the labels themselves.
	 */
	public void updateDisplays()
	{
		startOfInterval = statsPanel.getStartOfInterval();
		tradeHistory = flippingItem.getIntervalHistory(startOfInterval);
		totalProfit = flippingItem.currentProfit(startOfInterval);
		totalExpense = flippingItem.getCashflow(tradeHistory, true);
		totalRevenue = flippingItem.getCashflow(tradeHistory, false);
		itemCountFlipped = flippingItem.countItemsFlipped(tradeHistory);

		updateTitleDisplay();
		updateItemSubInfosDisplay();
	}

	/* Total profit and name label */
	private void updateTitleDisplay()
	{
		//Make sure the item name fits. This has to be called BEFORE the label is made.
		nameTitleLabel.setPreferredSize(new Dimension(0, 0));
		nameTitleLabel = new JLabel(flippingItem.getItemName());
		nameTitleLabel.setBorder(new EmptyBorder(0, 0, 2, 0));

		String totalProfitString = ((totalProfit > 0) ? "+" : "") + UIUtilities.quantityToRSDecimalStack(totalProfit, true) + " gp";

		if (itemCountFlipped != 0)
		{
			totalProfitString += " (x " + QuantityFormatter.formatNumber(itemCountFlipped) + ")";
		}

		itemProfitTitleLabel.setText(totalProfitString);
		itemProfitTitleLabel.setForeground((totalProfit >= 0) ? ColorScheme.GRAND_EXCHANGE_PRICE : UIUtilities.OUTDATED_COLOR);
		itemProfitTitleLabel.setBorder(new EmptyBorder(0, 0, 2, 0));
		itemProfitTitleLabel.setFont(FontManager.getRunescapeSmallFont());
	}

	private void updateItemSubInfosDisplay()
	{
		totalProfitValLabel.setText(UIUtilities.quantityToRSDecimalStack(totalProfit, true) + " gp");
		totalProfitValLabel.setForeground((totalProfit >= 0) ? ColorScheme.GRAND_EXCHANGE_PRICE : UIUtilities.OUTDATED_COLOR);

		quantityValLabel.setText(itemCountFlipped + " Items");

		avgBuyPriceValLabel.setText(QuantityFormatter.formatNumber((int) (totalExpense / itemCountFlipped)) + " gp");
		avgSellPriceValLabel.setText(QuantityFormatter.formatNumber((int) (totalRevenue / itemCountFlipped)) + " gp");

		if (!tradeHistory.isEmpty())
		{
			OfferInfo lastRecordedTrade = tradeHistory.get(tradeHistory.size() - 1);
			timeOfLastFlipValLabel.setText(UIUtilities.formatDuration(lastRecordedTrade.getTime()) + " ago");
			timeOfLastFlipValLabel.setToolTipText(UIUtilities.formatTime(lastRecordedTrade.getTime(), plugin.getConfig().twelveHourFormat(), true));
		}

		float roi = (float) totalProfit / totalExpense * 100;

		roiValLabel.setText(String.format("%.2f", roi) + "%");
		roiValLabel.setForeground(UIUtilities.gradiatePercentage(roi, plugin.getConfig().roiGradientMax()));
		profitEachValLabel.setText(UIUtilities.quantityToRSDecimalStack((totalProfit / itemCountFlipped), true) + " gp/ea");
		profitEachValLabel.setForeground((totalProfit >= 0) ? ColorScheme.GRAND_EXCHANGE_PRICE : UIUtilities.OUTDATED_COLOR);
	}

}