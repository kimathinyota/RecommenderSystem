# RecommenderSystem

**Full write up in: RecommenderReport.pdf** <br/>

An implementation of a recommender system with different possible parameters and running an experiment to find the optimal system.


## Goal

* We are given a csv data set of thousands of user ratings of items in the form (UserID, ItemID, Rating, Timestamp)
* We are given a small data set of users and items they haven't yet rated (UserID, ItemID, Rating, Timestamp)
* The goal is to determine the missing ratings of items in the previous data set
* For the purpose of evaluating our Recommender systems, we also have a separate data set of the true ratings of users
* An experiment was run by adjusting possible parameters to our implementation to determine the optimal Recommender system

Since we had fewer items than users in our data set, we chose Item Based Collaborative Filtering for our recommender
system.

## Data Representation

* We loaded the large csv data sets into a MySQL database
* We read the database into our program via Hashampas
  * One maps an item to a map of users to ratings
  * Map of users to a list of item records (in the form of ItemID, Rating and Timestamp) and the
    total rating
* A similarity matrix (sim[item][users]) was represented using two one-dimensional arrays: 
  * one array for storing all the similarity values 
  * another array for storing the indexes for each item allowing for easy
    access of similarity in the form of (item, user) shown
* The calculated similarity matrix was stored as a .ser file object in secondary storage for quick access


## Similarity Measure

We used the adjusted cosine similarity for determining similarity of items. 

Possible ranges
1. -1 to 1: Default similarity ranging from -1, which is the least similar, to 1 being the most similar. 
2. 0 to 1: The second similarity range, produced via shifting the previous one with the assumption that when the denominator of the
prediction is 0, there are too few ratings to accurately produce a predicted rating, and it is
possible for negative similarity values to incorrectly trigger this assumption.


   
### Neighbourhood Selection
The predicted score for a given user and item will be calculated from a neighbourhood set of
items that are similar to the input item. In our implementation, the neighbourhood set will
consist of the k most similar/recent items.


#### Parameters
The following parameters were considered in the experiment:

1. Highest similarity: the k items with the highest similarity were selected as neighbours
2. Most Recent: items that were rated in the most similar time period were chosen, since we assumed that time
   period has an effect on an item’s rating
3. Most Recent and Highest Similarity to see if the combination of both metrics will result

## Cold Starts
Cold start occurs when a prediction value could not be reached due to a limited number of
ratings to produce a prediction.

#### Parameters
To overcome this problem, we came up with 2 methods:
1. Median value of all the user ratings as the prediction for the user
2. Average value of ratings

## Experiment

The aim of this experiment is to find the configuration of parameters that will have the best
outcome. The outcome will be based on the following metrics.

* MSE (Mean Squared Error): refers to the squared difference of the
  predicted values and the actual value.
* Number of Exact Matches
* Processing speed of configuration

