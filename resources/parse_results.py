import sys
import json
import numpy as np


def _error(actual, predicted):
    """ Simple error """
    return actual - predicted


def _percentage_error(actual, predicted):
    """
    Percentage error
    Note: result is NOT multiplied by 100
    """
    return _error(actual, predicted) / actual


def parse_results(input_filename, output_filename):
    with open(input_filename) as f:
        with open(output_filename, 'w') as fw:
            fw.write("query\tactualResult\tpercentage_error\tqueryTriples\tscale\tepsilon\tk\telasticStability\ttripleSelectivity\n")
            for line in f.readlines():
                query_results = json.loads(line)
                private_result = query_results['privateResult']
                actual_result = np.array(query_results['result'])
                if(actual_result != 0):
                    percentage_error = _percentage_error(np.array([actual_result,actual_result,actual_result,actual_result,actual_result,actual_result,actual_result,actual_result,actual_result,actual_result]), private_result)
                else:
                    percentage_error = 10000
                # print("actual {} error {}".format(actual_result, abs(np.mean(percentage_error)*100)))
                if('query' in query_results):
                    fw.write(str(query_results['query']) + str('\t'))
                fw.write(str(actual_result) + '\t')
                fw.write(str(abs(np.mean(percentage_error)*100)) + str('\t'))
                fw.write(str(query_results['queryTriples']) + str('\t'))
                fw.write(str(query_results['scale']) + str('\t'))
                fw.write(str(query_results['epsilon']) + str('\t'))
                fw.write(str(query_results['k']) + str('\t'))
                fw.write(str(query_results['elasticStability']['label']) + str('\t'))
                fw.write(str(query_results['tripleSelectivity']))
                fw.write('\n')


if __name__ == '__main__':
    if len(sys.argv) == 1:
        print('Usage: python parse_privacy_results.py JSON result_file output_file ')
    else:
        print(sys.argv[1], sys.argv[2])
        parse_results(sys.argv[1], sys.argv[2])
