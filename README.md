# QuickBlox-Tigase-CustomFeatures

A list of QuickBlox custom features for www.tigase.org 

tigase-server:
* QBAuth - custom implementation of AuthRepository
* CustomObjects plugin - save chat messages to QuickBlox CustomObjects module
* LastRequestAtPlugin - update user's lastRequestAt field on each chat presence
* MessageReadDelivered plugin - implementation of http://xmpp.org/extensions/xep-0333.html
* Custom MessageAmp,Message,OfflineMessages plugins to save chat messages to different storage
* Custom statistics collector

tigase-muc:
* Split rooms by applications logic
* Added 'getRoomOwner' method to DAO
* Customisations for GroupchatMessageModule class to save chat messages to different storage and to make room   join as not required

QBChatUtils:
* A list of small helpers for XMPP


